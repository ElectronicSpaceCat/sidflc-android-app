/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.greentech.plink.fragments.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.android.greentech.plink.R

private const val PERMISSION_REQUIRED = Manifest.permission.CAMERA

// NOTE: Use if wanting to check multiple permissions
//private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

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
        // Ensure we're good to start camera operation
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.container_camera).navigate(
                R.id.action_permissions_to_camera
            )
        }
    }

    companion object {
//        // NOTE: Use if wanting to check multiple permissions
//        /** Convenience method used to check if all permissions required by this app are granted */
//        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
//            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
//        }

        /** Convenience method used to check if all permissions required by this app are granted */
        fun hasPermission(context: Context) : Boolean{
            return ContextCompat.checkSelfPermission(context, PERMISSION_REQUIRED) == PackageManager.PERMISSION_GRANTED
        }
    }
}
