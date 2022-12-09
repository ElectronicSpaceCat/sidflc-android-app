package com.android.greentech.plink.fragments.cameraOverlay

import android.content.SharedPreferences
import android.graphics.Color
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.preference.PreferenceManager
import com.android.greentech.plink.R
import com.android.greentech.plink.databinding.WidgetBallisticsButtonBinding
import com.android.greentech.plink.fragments.dialogs.InputDialogFragment
import com.android.greentech.plink.utils.converters.ConverterData

/**
 * The purpose of this class is a generic handler for
 * ballistic buttons.
 *
 * Attributes:
 * - Lock status icon
 * - Clickable Auto/Manual setting
 * - Input dialog on button click if set for manual mode
 * - Button mode is stored as a preference
 * - Button text displays the unit current unit type
 * - Manual mode data is stored in preferences
 * - The button can be enabled/disabled
 * - The data can be enabled/disabled which toggles it's text color
 */
open class BallisticsButton<K : ConverterData<*, *>>(private val title: String, private val dialogTitle: String, button: WidgetBallisticsButtonBinding, data: K):
    InputDialogFragment.InputDialogListener {

    enum class IconId{
        MODE,
        SET,
        LOCK,
        NA
    }

    enum class DataStatus{
        NOT_SET,
        SET,
        NA
    }

    enum class LockStatus{
        LOCKED,
        UNLOCKED,
        DISABLED,
        NA
    }

    enum class Mode{
        AUTO,
        MANUAL,
        NA
    }

    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(button.root.context)
    private val prefModeTag = "_mode"
    private val prefLockTag = "_lock"
    private val _button = button
    private var _data = data
    private var _acquisitionMode = Mode.NA
    private var _lockStatus = LockStatus.NA
    private var _dataStatus = DataStatus.NA
    private var _btnEnabledForAuto = true
    private var _dataUpdateEnabled = false

    val acquisitionMode: Mode
        get() = _acquisitionMode

    val lockStatus : LockStatus
        get() = _lockStatus

    /**
     * Enable/Disable the button
     * Only works for AUTO mode
     */
    fun enable(value : Boolean) {
        if(_acquisitionMode == Mode.AUTO) {
            dataStatus = DataStatus.NOT_SET
            dataUpdateEnable = false
            _btnEnabledForAuto = value
            if (value) {
                _button.btn.setTextColor(Color.WHITE)
            } else {
                _button.btn.setTextColor(Color.DKGRAY)
            }
        }
    }

    /**
     * Enable/Disable the data value visual updating
     * Only works for AUTO mode, MANUAL mode disables this.
     */
    var dataUpdateEnable : Boolean
        get() = _dataUpdateEnabled
        set(value) {
            if(_acquisitionMode == Mode.AUTO){
                _dataUpdateEnabled = value
                if(!value){
                    _button.data.text = _button.root.context.getString(R.string.value_unknown)
                    _button.data.isEnabled = false
                }
                else{
                    _button.data.isEnabled = true
                }
            }
        }

    var dataStatus : DataStatus
        get() = _dataStatus
        set(value) {
            _dataStatus = value
            if (value == DataStatus.SET) {
                _button.btnStatusIcon.setImageLevel(DataStatus.SET.ordinal)
            } else {
                _button.btnStatusIcon.setImageLevel(DataStatus.NOT_SET.ordinal)
            }
        }

    private fun setLockStatus(lock: LockStatus){
        if(_lockStatus != lock) {
            _lockStatus = lock
            _button.btnLockIcon.setImageLevel(lock.ordinal)
        }
    }

    private fun getAutoModeLockStatus() : LockStatus {
        val lockStatus = prefs.getInt(title + prefLockTag, LockStatus.UNLOCKED.ordinal)
        return if(lockStatus < LockStatus.values().size){
            LockStatus.values()[lockStatus]
        } else {
            LockStatus.NA
        }
    }

    private fun setAndStoreAutoModeLockStatus(lockStatus: LockStatus) {
        // If in auto mode then set the lock and update the icon
        if(_acquisitionMode == Mode.AUTO) {
            setLockStatus(lockStatus)
        }

        // If the requested lock status is not the current then store it
        if(getAutoModeLockStatus() != lockStatus) {
            val editor = PreferenceManager.getDefaultSharedPreferences(_button.root.context).edit()
            editor.putInt(title + prefLockTag, lockStatus.ordinal).apply()
        }
    }

    fun setIconVisibility(icon : IconId, visibility : Int){
        val locVis = if(visibility != View.VISIBLE && visibility != View.INVISIBLE && visibility != View.GONE){
            View.VISIBLE
        }
        else{
            visibility
        }

        when(icon){
            IconId.MODE -> {
                _button.mode.visibility = locVis
            }
            IconId.SET -> {
                _button.set.visibility = locVis
            }
            IconId.LOCK -> {
                _button.lock.visibility = locVis
            }
            else -> {}
        }
    }

    private fun setAcquisitionMode(value : Mode) {
        if(_acquisitionMode != value) {
            _acquisitionMode = value
            when(_acquisitionMode) {
                Mode.AUTO -> {
                    _button.acquisitionModeValue.text = "A"
                    setLockStatus(getAutoModeLockStatus())
                    dataUpdateEnable = false
                    dataStatus = DataStatus.NOT_SET
                }
                Mode.MANUAL -> {
                    _button.acquisitionModeValue.text = "M"
                    _data.loadFromPrefs(_button.root.context)
                    _button.data.text = _data.valueStr()
                    setLockStatus(LockStatus.DISABLED)
                    _button.btn.setTextColor(Color.WHITE)
                    _button.data.isEnabled = true
                    dataUpdateEnable = false
                    dataStatus = DataStatus.SET
                }
                else -> {

                }
            }
            val editor = PreferenceManager.getDefaultSharedPreferences(_button.root.context).edit()
            editor.putInt(title + prefModeTag, _acquisitionMode.ordinal).apply()
        }
    }

    private fun onLockClick() {
        when(_lockStatus){
            LockStatus.LOCKED -> {
                setAndStoreAutoModeLockStatus(LockStatus.UNLOCKED)
            }
            LockStatus.UNLOCKED -> {
                setAndStoreAutoModeLockStatus(LockStatus.LOCKED)
            }
            else -> {}
        }
    }

    open fun onButtonClick() {
        if(_acquisitionMode == Mode.MANUAL) {
            try {
                val fragmentManager: FragmentManager = (_button.root.context as FragmentActivity).supportFragmentManager
                // Create and show input dialog
                InputDialogFragment(
                    dialogTitle,
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
                    _data.value,
                    7,
                    2,
                    this
                ).show(fragmentManager, null)
            } catch (e: ClassCastException) {
                Log.e("Ballistics Button", "Can't get fragment manager")
            }
        }
    }

    open fun onButtonClickDisabled() {

    }

    open fun onButtonClickDataPaused() {

    }

    open fun onAcquisitionModeClick(){
        when(_acquisitionMode){
            Mode.AUTO -> {
                setAcquisitionMode(Mode.MANUAL)
            }
            Mode.MANUAL -> {
                setAcquisitionMode(Mode.AUTO)
            }
            else -> {}
        }
    }

    override fun onDialogPositiveClick(value: Number) {
        _data.setValue(value.toDouble())
        _data.storeToPrefs(_button.root.context)
        _button.data.text = _data.valueStr()
    }

    override fun onDialogNegativeClick(value: Number) {

    }

    init {
        val mode = prefs.getInt(title + prefModeTag, Mode.AUTO.ordinal)
        if(mode < Mode.values().size){
            setAcquisitionMode(Mode.values()[mode])
        }

        // Setup onClick listener for main button
        _button.btn.setOnClickListener {
            if(_btnEnabledForAuto || acquisitionMode == Mode.MANUAL) {
                if (_button.data.isEnabled) {
                    onButtonClick()
                } else {
                    onButtonClickDataPaused()
                }
            }
            else{
                onButtonClickDisabled()
            }
        }

        // Setup onClick listener for mode button
        _button.mode.setOnClickListener {
            onAcquisitionModeClick()
        }

        // Setup onClick listener lock button
        _button.lock.setOnClickListener {
            onLockClick()
        }

        // Observe the value on change
        _data.valueOnChange.observe(_button.root.findViewTreeLifecycleOwner()!!){
            if(_dataUpdateEnabled){
                _button.data.text = _data.valueStr()
            }
        }

        // Observe the value unit on change
        _data.unitOnChange.observe(_button.root.findViewTreeLifecycleOwner()!!){
            (title + " " + "(" + _data.unitStr() + ")").also {  _button.btn.text = it }
        }
    }
}