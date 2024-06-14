package com.codewithkael.firebasevideocall.ui


import android.animation.ObjectAnimator
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.firebasevideocall.BuildConfig
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.UsbReceiver
import kotlin.properties.Delegates


class SplashAcitivity : AppCompatActivity() {
    lateinit var sharedPref: SharedPreferences
    lateinit var usbReceiver: UsbReceiver
    lateinit var shEdit: SharedPreferences.Editor
    var isLogin by Delegates.notNull<Boolean>()
    val handler: Handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_acitivity)
         usbReceiver = UsbReceiver()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction("android.hardware.usb.action.USB_STATE")
        registerReceiver(usbReceiver, filter)
        val version =
            findViewById<TextView>(R.id.version)
        version.text = BuildConfig.VERSION_NAME

        try {
            val intent = Intent()
            val manufacturer = Build.MANUFACTURER
            if ("xiaomi".equals(manufacturer, ignoreCase = true)) {
                intent.setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            } else if ("oppo".equals(manufacturer, ignoreCase = true)) {
                intent.setComponent(
                    ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                )
                //intent.setComponent(new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerConsumptionActivity"));
            } else if ("vivo".equals(manufacturer, ignoreCase = true)) {
                intent.setComponent(
                    ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                )
            } else if ("huawei".equals(manufacturer, ignoreCase = true)) {
                intent.setComponent(
                    ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                )
            } else {
                // applySubmit(false);
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val progressBar =
            findViewById<ProgressBar>(R.id.progressBar) // Set up the rotation animation  // Duration in milliseconds // Infinite repeat  // Restart the animation// Start the animation
        val rotation = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f);
        rotation.setDuration(2000);
        rotation.repeatCount = ObjectAnimator.INFINITE;
        rotation.repeatMode = ObjectAnimator.RESTART;
        rotation.start();

        handler.postDelayed(runHandler, 2000)

    }

    val runHandler = {
       /* if ( isVerificationStatusTrue()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
           finish()
        }*/
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun isVerificationStatusTrue(): Boolean {
        sharedPref = applicationContext.getSharedPreferences("see_for_me", MODE_PRIVATE)
        return sharedPref.getBoolean("is_login", false)
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runHandler)
        unregisterReceiver(usbReceiver)
    }
    override fun onDestroy() {
        super.onDestroy()

    }
}