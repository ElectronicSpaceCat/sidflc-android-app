package com.android.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.android.app.utils.misc.Utils
import com.android.app.databinding.ActivitySplashScreenBinding

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var activitySplashScreenBinding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activitySplashScreenBinding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(activitySplashScreenBinding.root)

        // Keep screen from going to sleep
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        Utils.hideActionAndNavBar(this, Utils.SysBarView.HIDE_BOTH)

        val manager: PackageManager = this.packageManager
        val info: PackageInfo = manager.getPackageInfo(this.packageName, PackageManager.GET_ACTIVITIES)

        activitySplashScreenBinding.splashAppVersion.text = info.versionName

        // Jump to SensorsActivity after DURATION milliseconds
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(intent)
            finish() // <- This prevents the splash screen from showing up if pressing back (removes from stack)
        }, DURATION.toLong())
    }

    companion object {
        private const val DURATION = 1200
    }
}
