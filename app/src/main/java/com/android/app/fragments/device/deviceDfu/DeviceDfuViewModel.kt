package com.android.app.fragments.device.deviceDfu

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.app.R
import com.android.app.dataShared.DataShared
import com.android.app.utils.network.ConnectionLiveData
import com.android.app.utils.network.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nordicsemi.android.ble.livedata.state.ConnectionState
import no.nordicsemi.android.dfu.DfuServiceInitiator
import java.io.File
import java.io.IOException

class DeviceDfuViewModel(application: Application) : AndroidViewModel(application) {
    private var mFileStreamUri : Uri ?= null
    private var mFilePath = application.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path + "/" + application.getString(R.string.fw_file_name)

    private var _latestVersion = ""
    private var _downloadProgress = MutableLiveData(0)
    private var _updateStatus = MutableLiveData(UpdateStatus.NA)

    val networkStatus = ConnectionLiveData(application)

    val updateStatusLive : LiveData<UpdateStatus>
        get() = _updateStatus
    var updateStatus : UpdateStatus
        get() = _updateStatus.value!!
        set(value) {
            _updateStatus.postValue(value)
        }

    val downloadProgressLive : LiveData<Int>
        get() = _downloadProgress

    val deviceConnectionState : LiveData<ConnectionState>
        get() = DataShared.device.connectionState

    val latestVersion : String
        get() = _latestVersion


    fun isBackupDFU(context: Context) : Boolean {
        return (DataShared.device.name != context.getString(R.string.app_name))
    }

    fun startFirmwareUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            updateStatus = UpdateStatus.DOWNLOADING
            // Download firmware which is a blocking function
            updateStatus = if(downloadFirmware(context)) {
                UpdateStatus.DOWNLOADED
            }
            else {
                UpdateStatus.ERROR
            }
            // Update the firmware
            updateStatus = UpdateStatus.UPDATING
            updateFirmware(context)
        }
    }

    fun cleanUp() {
        updateStatus = UpdateStatus.NA
        deleteFile(mFilePath)
    }

    fun checkFirmwareVersion(context: Context) {

        updateStatus = UpdateStatus.CHECKING_FIRMWARE

        if(networkStatus.value == null){
            return
        }

        if(!networkStatus.value!!) {
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            // Get latest version
            try {
                _latestVersion = Utils.httpGet(context.getString(R.string.fw_file_version))
            }
            catch (e : Exception) {
                return@launch
            }

            // If device is in DFU mode then allow any firmware
            if(DataShared.device.isBackupBootloader
                || DataShared.device.versionFirmware.value!! == ""){
                updateStatus = UpdateStatus.UPDATE_AVAILABLE
                return@launch
            }

            val versionDataNew = _latestVersion.split(".")
            val versionDataCurrent = DataShared.device.versionFirmware.value!!.split(".")

            if(versionDataNew.size == versionDataCurrent.size) {
                // Do a simple check if major, minor, or revision parts are greater than the current
                for (idx in 0..versionDataNew.lastIndex) {
                    val newInt = versionDataNew[idx].toIntOrNull()
                    val currentInt = versionDataCurrent[idx].toInt()

                    if (newInt == null) {
                        updateStatus = UpdateStatus.ERROR
                        return@launch
                    }

                    if(newInt > currentInt){
                        updateStatus = UpdateStatus.UPDATE_AVAILABLE
                        return@launch
                    }
                }

                updateStatus = UpdateStatus.ON_LATEST_FIRMWARE
            }
            else{
                return@launch
            }
        }
    }

    private suspend fun downloadFirmware(context: Context) : Boolean {
        // Download the latest firmware
        return try {
            Utils.download(
                context.getString(R.string.fw_file_download), mFilePath) { progress, length ->
                // Update the progress bar when downloading
                _downloadProgress.postValue(((progress / length) * 100).toInt())
            }
            true
        } catch (e : Exception) {
            false
        }
    }

    private fun updateFirmware(context: Context) {
        // Define the starter for the DFU handler
        // TODO: Look into the settings
        val starter = DfuServiceInitiator(DataShared.device.address)
            .setDeviceName(DataShared.device.name)
            .setKeepBond(true)
            .setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true)
            .setPrepareDataObjectDelay(300L)
            .setZip(mFileStreamUri, mFilePath)
            .setScope(DfuServiceInitiator.SCOPE_APPLICATION) // NOTE: This will likely change

        // Start the DFU handler
        starter.start(context, DeviceDfuService::class.java)
    }

    /**
     * Delete file if exists
     */
    private fun deleteFile(filePath : String) {
        try {
            File(filePath).delete()
        }
        catch (e : Exception) {
            when (e) {
                is SecurityException  -> {}
                is IOException -> {}
                else -> {}
            }
        }
    }

    companion object{
        enum class UpdateStatus {
            NO_NETWORK,
            CHECKING_FIRMWARE,
            ON_LATEST_FIRMWARE,
            UPDATE_AVAILABLE,
            DOWNLOADING,
            DOWNLOADED,
            UPDATING,
            ERROR,
            NA
        }
    }

    init {
        cleanUp()
    }
}