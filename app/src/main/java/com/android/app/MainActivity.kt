package com.android.app

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.navigation.ui.AppBarConfiguration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.ui.NavigationUI
import androidx.preference.PreferenceManager
import com.android.app.databinding.ActivityMainBinding
import com.android.app.dataShared.DataShared
import com.android.app.device.projectile.ProjectilePrefUtils
import com.android.app.utils.misc.Utils

private const val IMMERSIVE_FLAG_TIMEOUT = 500L

/**
 * Main entry point into our app. This app follows the single-activity pattern, and all
 * functionality is implemented in the form of fragments.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    private lateinit var dataShared : DataShared

    private lateinit var _cameraFragment : Fragment
    private lateinit var _cameraOverlayFragment : Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        // Keep screen from going to sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Force orientation to portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR

        // Hide the status bar
        Utils.hideActionAndNavBar(this, Utils.SysBarView.HIDE_STATUS_BAR)

        // Set the toolbar
        setSupportActionBar(activityMainBinding.main.toolbar)

        // Setup navigation controller
        val drawer = activityMainBinding.drawer
        val navigationView = activityMainBinding.navView

        // Get the nav controller for the main nav graph
        navController = Utils.getNavController(supportFragmentManager, R.id.container_nav)

        // Get the homeFragment menu item which is the first element in the menu list
        val homeFragment = navigationView.menu[0]
        // Initialize the selected menu item to the homeFragment
        homeFragment.isChecked = true

        // Set up a navigation drawer listener
        navigationView.setNavigationItemSelectedListener { item ->
            val options : NavOptions = if (item.itemId == homeFragment.itemId) {
                // Pop up to home and set inclusive
                NavOptions.Builder()
                    .setPopUpTo(homeFragment.itemId, true)
                    .setLaunchSingleTop(true)
                    .build()
            } else{
                // Pop up to home before navigating to next screen and set non-inclusive
                NavOptions.Builder()
                    .setPopUpTo(homeFragment.itemId, false)
                    .setLaunchSingleTop(true)
                    .build()
            }
            navController.navigate(item.itemId, null, options)
            drawer.closeDrawer(navigationView)
            true
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration =
            AppBarConfiguration.Builder(R.id.homeFragment)
                .setOpenableLayout(drawer)
                .build()

        // Bind the actionbar and nav menu to the nav controller
        NavigationUI.setupActionBarWithNavController(this, navController, drawer)

        // Get the camera related fragments
        _cameraFragment = supportFragmentManager.findFragmentById(R.id.container_camera)!!
        _cameraOverlayFragment = supportFragmentManager.findFragmentById(R.id.container_camera_overlay)!!

        // Attach/Detach the camera/cameraOverlay fragment view holders when navigating away from
        // the home screen.
        //
        // These fragments are handled like this because they require the full screen and not be
        // offset by the navigation handler action bar.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // If id is a menu item then set it as 'checked' it in the nav drawer
            navigationView.setCheckedItem(destination.id)

            if(destination.id == R.id.homeFragment){
                supportFragmentManager.beginTransaction().attach(_cameraFragment).commit()
                supportFragmentManager.beginTransaction().attach(_cameraOverlayFragment).commit()
            }
            else {
                supportFragmentManager.beginTransaction().detach(_cameraFragment).commit()
                supportFragmentManager.beginTransaction().detach(_cameraOverlayFragment).commit()
            }
        }

        // Get internal preference flag that determines default preference values have been initialized
        val defaultValueSp: SharedPreferences = this.getSharedPreferences(
            PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, MODE_PRIVATE
        )

        // Note: this only will be set true the very first time the application is run after installation
        // Has preferences been previously initialized?
        if (!defaultValueSp.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)){
            // Init preferences on first time application starts
            PreferenceManager.setDefaultValues(this, R.xml.prefs_main, false)
            // Init any additional defaults preferences here
            ProjectilePrefUtils.setDefaultProjectiles(this)
        }

        // Init the shared data
        dataShared = DataShared(this)
    }

    override fun onResume() {
        // NOTE: Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        //  be trying to set app to immersive mode before it's ready and the flags do not stick
        activityMainBinding.drawer.postDelayed({
            Utils.hideActionAndNavBar(this, Utils.SysBarView.HIDE_STATUS_BAR)
        }, IMMERSIVE_FLAG_TIMEOUT)
        super.onResume()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
