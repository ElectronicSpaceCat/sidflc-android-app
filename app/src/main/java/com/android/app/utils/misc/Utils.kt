package com.android.app.utils.misc

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager

object Utils {
    private const val PREFS_LOCATION_NOT_REQUIRED = "location_not_required"
    private const val PREFS_PERMISSION_REQUESTED = "permission_requested"

    enum class SysBarView{
        HIDE_NAV_BAR,
        HIDE_STATUS_BAR,
        HIDE_BOTH
    }

    /**
     * Checks whether a permission is granted.
     *
     * @param context
     * @param permission
     * @return true if permission granted
     */
    fun hasPermission(context: Context, permission: String) : Boolean{
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks whether a permission is granted.
     *
     * @param context
     * @param permissions
     * @return true if permission granted
     */
    fun hasPermission(context: Context, permissions : Array<String>) : Boolean {
        return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * Checks whether Bluetooth is enabled.
     *
     * @return true if Bluetooth is enabled, false otherwise.
     */
    fun isBleEnabled(context: Context): Boolean {
        val mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val mBtAdapter = mBluetoothManager.adapter
        return mBtAdapter != null && mBtAdapter.isEnabled
    }

    /**
     * Checks for required permissions.
     *
     * @return True if permissions are already granted, false otherwise.
     */
    fun isLocationPermissionsGranted(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
    }

    /**
     * Returns true if location permission has been requested at least twice and
     * user denied it, and checked 'Don't ask again'.
     *
     * @param activity the activity.
     * @return True if permission has been denied and the popup will not come up any more,
     * false otherwise.
     */
    fun isLocationPermissionDeniedForever(activity: Activity): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        return (!isLocationPermissionsGranted(activity) // Location permission must be denied
                && preferences.getBoolean(PREFS_PERMISSION_REQUESTED, false) // Permission must have been requested before
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    /**
     * On some devices running Android Marshmallow or newer location services must be enabled in
     * order to scan for Bluetooth LE devices. This method returns whether the Location has been
     * enabled or not.
     *
     * @return True on Android 6.0+ if location mode is different than LOCATION_MODE_OFF.
     * It always returns true on Android versions prior to Marshmallow.
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * Location enabled is required on some phones running Android Marshmallow or newer
     * (for example on Nexus and Pixel devices).
     *
     * @param context the context.
     * @return False if it is known that location is not required, true otherwise.
     */
    fun isLocationRequired(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getBoolean(PREFS_LOCATION_NOT_REQUIRED, false)
    }

    /**
     * When a Bluetooth LE packet is received while Location is disabled it means that Location
     * is not required on this device in order to scan for LE devices. This is a case of Samsung
     * phones, for example. Save this information for the future to keep the Location info hidden.
     *
     * @param context the context.
     */
    fun markLocationNotRequired(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_LOCATION_NOT_REQUIRED, false).apply()
    }

    /**
     * The first time an app requests a permission there is no 'Don't ask again' checkbox and
     * [ActivityCompat.shouldShowRequestPermissionRationale] returns false.
     * This situation is similar to a permission being denied forever, so to distinguish both cases
     * a flag needs to be saved.
     *
     * @param context the context.
     */
    fun markLocationPermissionRequested(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(PREFS_PERMISSION_REQUESTED, true).apply()
    }

    fun hideActionAndNavBar(activity: Activity?, set: SysBarView) {
        if (activity == null) {
            return
        }

        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).let { controller ->
            when (set) {
                SysBarView.HIDE_NAV_BAR -> controller.hide(WindowInsetsCompat.Type.navigationBars())
                SysBarView.HIDE_STATUS_BAR -> controller.hide(WindowInsetsCompat.Type.statusBars())
                SysBarView.HIDE_BOTH -> controller.hide(WindowInsetsCompat.Type.systemBars())
            }
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun showKeyboard(activity: Activity, show: Boolean) {
        val view = activity.currentFocus
        val methodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        assert(view != null)
        if (show) {
            methodManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } else {
            methodManager.hideSoftInputFromWindow(
                view!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    /**
     * Get bluetooth device if the name matches one in the list of bonded devices
     *
     * @param context
     * @param name
     * @return BluetoothDevice if match found, null if not
     */
    @SuppressLint("MissingPermission")
    fun getBondedDeviceByName(context: Context, name : String) : BluetoothDevice? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if(manager.adapter == null) {
            return null
        }
        if(!manager.adapter.isEnabled) {
            return null
        }
        val devices = manager.adapter.bondedDevices
        devices.forEach { device ->
            if (device.bondState == BluetoothDevice.BOND_BONDED
                && device.name == name){
                return device
            }
        }
        return null
    }

    // NOTE: This takes care of a bug when calling
    //  "val navController = Navigation.findNavController(this, R.id.container)"
    fun getNavController(fragmentManager: FragmentManager, layoutID: Int): NavController {
        val fragment = fragmentManager.findFragmentById(layoutID)
        check(fragment is NavHostFragment) {
            ("Activity $this does not have a NavHostFragment")
        }
        return fragment.navController
    }

    /**
     * Covert string to double
     *
     * @param valueStr
     * @return value - Numerical value, if invalid, 0.0 will be returned.
     */
    fun convertStrToDouble(valueStr: String?) : Double {
        var retVal = valueStr?.toDoubleOrNull()
        if(null == retVal){
            retVal = 0.0
        }
        return retVal
    }

    /**
     * Covert string to float
     *
     * @param valueStr
     * @return value - Numerical value, if invalid, 0.0 will be returned.
     */
    fun convertStrToFloat(valueStr: String?) : Float {
        var retVal = valueStr?.toFloatOrNull()
        if(null == retVal){
            retVal = 0.0f
        }
        return retVal
    }

    fun setActionBarTitle(activity: AppCompatActivity, title: Int){
        activity.supportActionBar?.title = activity.getString(title)
    }

    fun intToBytes(bytes: ByteArray, offset: Int, value: Int) {
        var loffset = offset
        bytes[loffset++] = (value shl 24 shr 24).toByte()
        bytes[loffset++] = (value shl 16 shr 24).toByte()
        bytes[loffset++] = (value shl 8 shr 24).toByte()
        bytes[loffset] = (value shr 24).toByte()
    }

    fun showToast(context: Context, message: CharSequence, duration: Int, gravity: Int, xOffset: Int, yOffset: Int) {
        val toast = Toast.makeText(context, message, duration)
        toast.setGravity(gravity, xOffset, yOffset)
        toast.show()
    }
}
