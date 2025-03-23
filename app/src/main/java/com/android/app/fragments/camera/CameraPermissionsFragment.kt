package com.android.app.fragments.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import com.android.app.R
import com.android.app.utils.misc.Utils
import kotlinx.coroutines.launch
import androidx.navigation.findNavController

/**
 * The sole purpose of this fragment is to request permissions and, once granted, display the
 * camera fragment to the user.
 */
class CameraPermissionsFragment : Fragment() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean -> this.onRequestPermissionsResult(isGranted)
    }

//    // NOTE: Use if wanting to check multiple permissions
//    private val requestPermissionsLauncher =
//            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantStates: Map<String, Boolean> ->
//                for ((permission, granted) in grantStates) {
//                    if(granted){
//                        Toast.makeText(context, "$permission - granted", Toast.LENGTH_LONG).show()
//                    }
//                    else{
//                        Toast.makeText(context, "$permission - denied", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasPermission(requireContext())) {
            // Request camera-related permissions
            requestPermissionLauncher.launch(PERMISSION_REQUIRED)
            //requestPermissionsLauncher.launch(PERMISSIONS_REQUIRED)
        } else {
            // If permissions have already been granted, proceed
            navigateToCamera()
        }
    }

    private fun onRequestPermissionsResult(isGranted: Boolean) {
        if (isGranted) {
            // Take the user to the success fragment when permission is granted
            Toast.makeText(context, "Permission request granted", Toast.LENGTH_LONG).show()
            navigateToCamera()
        } else {
            Toast.makeText(context, "Permission request denied", Toast.LENGTH_LONG).show()
        }
    }

    private fun navigateToCamera() {
        // Create a new coroutine in the lifecycleScope
        lifecycleScope.launch {
            // repeatOnLifecycle launches the block in a new coroutine every time the
            // lifecycle is in the STARTED state (or above) and cancels it when it's STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                requireActivity().findNavController(R.id.container_camera).navigate(
                    R.id.action_permissions_to_camera
                )
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUIRED = Manifest.permission.CAMERA

//        // NOTE: Use if wanting to check multiple permissions
//        private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

//        // NOTE: Use if wanting to check multiple permissions
//        /** Convenience method used to check if all permissions required by this app are granted */
//        fun hasPermissions(context: Context) : Boolean {
//            return Utils.hasPermission(context, PERMISSIONS_REQUIRED)
//        }

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermission(context: Context) : Boolean{
            return Utils.hasPermission(context, PERMISSION_REQUIRED)
        }
    }
}
