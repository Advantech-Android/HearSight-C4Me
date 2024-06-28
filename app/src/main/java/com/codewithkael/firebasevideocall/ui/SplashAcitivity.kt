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
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.firebasevideocall.BuildConfig
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.UsbReceiver
import com.codewithkael.firebasevideocall.utils.setViewFields
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlin.properties.Delegates


class SplashAcitivity : AppCompatActivity() {
    lateinit var sharedPref: SharedPreferences
    lateinit var usbReceiver: UsbReceiver
    lateinit var shEdit: SharedPreferences.Editor
    var isLogin by Delegates.notNull<Boolean>()
    val handler: Handler = Handler(Looper.getMainLooper())
    private  val TAG = "SplashAcitivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_acitivity)
        usbReceiver = UsbReceiver()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction("android.hardware.usb.action.USB_STATE")
        registerReceiver(usbReceiver, filter)
        val showDialog = intent.getBooleanExtra("showDialog", false)

        //shared pref set
        sharedPref = applicationContext.getSharedPreferences(setViewFields.PREF_NAME, MODE_PRIVATE)
        shEdit=sharedPref.edit()
        val version =
            findViewById<TextView>(R.id.version)
        version.text = BuildConfig.VERSION_NAME

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
    if(isVerificationStatusTrue())
    {
    val intent=Intent(this@SplashAcitivity,MainActivity::class.java).apply {
        putExtra(setViewFields.EXTRA_USER_NAME,getData(setViewFields.KEY_USER_NAME,String::class.java) as String)
        putExtra(setViewFields.EXTRA_USER_PHONE,getData(setViewFields.KEY_USER_PHONE,String::class.java) as String)
    }
        startActivity(intent)
       // finish()
    }
    else
    {
        startActivity(Intent(this, LoginActivity::class.java))
      //  finish()
    }
        //startActivity(Intent(this, LoginActivity::class.java))
    }

    fun <T> getData(key:String,type:Class<T>):Any?{
        return when (type)
        {
            Int::class.java->sharedPref.getInt(key,0)
            String::class.java->sharedPref.getString(key,"")
            Boolean::class.java->sharedPref.getBoolean(key,false)
            else->null
        }
    }

    fun putData(key: String,value:Any){
        when(value){
        is Int->shEdit.putInt(key, value)
        is String->shEdit.putString(key, value)
        is Boolean->shEdit.putBoolean(key, value)
        }
        shEdit.apply()
    }

    private fun isVerificationStatusTrue(): Boolean {
        return getData(setViewFields.KEY_IS_LOGIN,Boolean::class.java) as Boolean
    }

    private fun updateFirebaseLoginStatus(isLogin: Boolean)
    {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null)
        {
            val database = FirebaseDatabase.getInstance().getReference("users/${user.uid}")
            database.child(setViewFields.KEY_IS_LOGIN).setValue(isLogin)
        }
    }


    override fun onResume() {
        super.onResume()

    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runHandler)

    }
    override fun onDestroy() {
        super.onDestroy()
    unregisterReceiver(usbReceiver)
    }
}