package com.android.app.fragments.device.deviceBallistics.dataPointsAdapter

import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.android.app.R
import com.android.app.databinding.ItemImpactDistanceBinding
import com.android.app.fragments.device.deviceBallistics.DeviceBallisticsFragment
import com.android.app.fragments.dialogs.InputDialogFragment
import com.android.app.utils.misc.Utils
import com.android.app.utils.prefs.PrefUtils
import java.math.RoundingMode

class DataPointsAdapter(fragment: DeviceBallisticsFragment): RecyclerView.Adapter<DataPointsAdapter.ViewHolder>() {
    private var _fragment : DeviceBallisticsFragment = fragment

    private val _prefsRecDataValueKey = _fragment.requireContext().getString(R.string.PREFERENCE_FILTER_TEST_RECORDED_DATA_VALUES)

    data class DataPoint(var pos : Double, var cal : Double, var rec : Double)

    private var _dataPoints = mutableListOf<DataPoint>()

    private val _onRecDataChanged = MutableLiveData(false)

    val onRecDataChanged : LiveData<Boolean>
        get() = _onRecDataChanged

    init {
        loadDataFromPrefs()
    }

    inner class ViewHolder(val dataPointBinding: ItemImpactDistanceBinding) :
        RecyclerView.ViewHolder(dataPointBinding.root) {
        init {
            dataPointBinding.recordedValue.setOnClickListener {
                showDataEditorDialog(this.adapterPosition)
            }
        }
    }

    private fun notifyRecDataChanged() {
        _onRecDataChanged.value = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val dataPointBinding = ItemImpactDistanceBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(dataPointBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dataPoint = _dataPoints[position]
        holder.dataPointBinding.posValue.text = dataPoint.pos.toString()
        holder.dataPointBinding.calculatedValue.text = dataPoint.cal.toString()
        holder.dataPointBinding.recordedValue.text = dataPoint.rec.toString()
    }

    fun addData(dataPoint : DataPoint){
        _dataPoints.add(dataPoint)
    }

    fun setPos(idx : Int, value : Double, notify : Boolean){
        if(idx < _dataPoints.size) {
            _dataPoints[idx].pos = value.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
            if(notify) {
                notifyItemChanged(idx)
            }
        }
    }

    fun setCal(idx : Int, value : Double, notify : Boolean){
        if(idx < _dataPoints.size) {
            _dataPoints[idx].cal = value.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
            if(notify) {
                notifyItemChanged(idx)
            }
        }
    }

    fun setRec(idx : Int, value : Double, notify : Boolean){
        if(idx < _dataPoints.size) {
            _dataPoints[idx].rec = value.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toDouble()
            if(notify) {
                notifyItemChanged(idx)
            }
        }
    }

    fun setDataList(dataPoints : List<DataPoint>){
        _dataPoints.clear()
        _dataPoints.addAll(dataPoints)
        notifyDataChange()
    }

    private fun clearData(){
        if(_dataPoints.isEmpty()) return

        val indexSizePrev = _dataPoints.lastIndex
        _dataPoints.clear()
        notifyItemRangeRemoved(0, indexSizePrev)
    }

    private fun notifyDataChange(){
        notifyItemRangeChanged(0, _dataPoints.lastIndex)
    }

    override fun getItemId(position: Int): Long {
        return _dataPoints[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return _dataPoints.size
    }

    fun getData() : List<DataPoint> {
        return _dataPoints
    }

    fun storeDataToPrefs() {
        var valueStrings: MutableList<String> ?= mutableListOf()

        if(_dataPoints.isNotEmpty()){
            // Create the string lists
            _dataPoints.forEach { idx ->
                valueStrings?.add(idx.rec.toString())
            }
        }
        else{
            valueStrings = null
        }

        // Store to prefs
        PrefUtils.addStringArrayToPrefs(_fragment.requireContext(), _prefsRecDataValueKey, valueStrings, ";")
    }

    private fun loadDataFromPrefs() {
        _dataPoints.clear()
        val recValues = PrefUtils.getStringArrayFromPrefs(_fragment.requireContext(), _prefsRecDataValueKey, ";") ?: return
        recValues.forEach {
            _dataPoints.add(DataPoint(0.0, 0.0, Utils.convertStrToDouble(it)))
        }
    }

    private fun showDataEditorDialog(position: Int) {
        val listener : InputDialogFragment.InputDialogListener = object : InputDialogFragment.InputDialogListener{
                override fun onDialogPositiveClick(value: Number) {
                    _dataPoints[position].rec = value as Double
                    notifyItemChanged(position)
                    notifyRecDataChanged()
                }
                override fun onDialogNegativeClick(value: Number) {
                    // Do nothing..
                }
            }

        // Show the dialog
        InputDialogFragment(
            _fragment.getString(R.string.data_point_input_title).plus(" ID: $position"),
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL,
            _dataPoints[position].rec,
            10,
            1,
            listener
        ).show(_fragment.parentFragmentManager, null)
    }
}