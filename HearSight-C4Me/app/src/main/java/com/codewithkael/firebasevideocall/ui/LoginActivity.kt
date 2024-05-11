package com.codewithkael.firebasevideocall.ui


import WebQ
import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.service.MainServiceActions
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.MySMSBroadcastReceiver
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject


private const val TAG = "***LoginActivity"
private const val STORAGE_PERMISSION_CODE = 123
private const val FILECHOOSER_RESULTCODE = 1

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    object uvc {

        var isUvc = MutableLiveData<Boolean>(false)
    }

    private lateinit var countryCode: String
    private var number: String? = null
    private lateinit var views: ActivityLoginBinding
    @Inject
    lateinit var mainRepository: MainRepository
    lateinit var webQ: WebQ
    lateinit var wifiManager: WifiManager
    lateinit var sharedPref:SharedPreferences
    lateinit var shEdit:SharedPreferences.Editor
    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    var mySMSBroadcastReceiver: MySMSBroadcastReceiver = MySMSBroadcastReceiver()
    companion object{
        var sms_otp=""
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        sharedPref = this.getSharedPreferences("save_login", MODE_PRIVATE)
        shEdit=sharedPref.edit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                mySMSBroadcastReceiver, IntentFilter("com.google.android.gms.auth.api.phone.SMS_RETRIEVED"), RECEIVER_VISIBLE_TO_INSTANT_APPS
            )
        }

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        init()

    }
private fun debugMode(){
         views.apply {
                Log.d(TAG, "onCreate: ${Build.BRAND}")
                if (Build.BRAND.equals("samsung", true)) {
                    usernameEt.setText("N")
                    passwordEt.setText("1234")
                } else {
                    usernameEt.setText("n")
                    passwordEt.setText("111")
                }
            }
}

    private fun clearAppData() {
        try {
            // clearing app data
            if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData() // note: it has a return value!
            } else {
                val packageName = applicationContext.packageName
                val runtime = Runtime.getRuntime()
                runtime.exec("pm clear $packageName")
               // autoStartActivity()
                //doRestart(this)
                triggerRebirth(applicationContext,intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun triggerRebirth(context: Context, nextIntent: Intent?) {
        val intent = Intent(context, LoginActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("Restart", Intent(this,LoginActivity::class.java))
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
        Runtime.getRuntime().exit(0)

    }
    private fun init() {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(this@LoginActivity,Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE),
                1000)
        }
try {
    number= tm.line1Number
    Log.d(TAG, "init: ${tm.simCountryIso} $number")
}catch (e:Exception){

}
        views.apply {
            countryCode = Locale.getDefault().country
            Log.d(TAG, "init: CountryCode:\t$countryCode")
            usernameEt.setText(getData("user_name"))
            passwordEt.setText(getData("user_phone"))
            btn.isEnabled = true
            clearData.setOnCheckedChangeListener(){buttonView, isChecked ->
                // isChecked will be true if checked, false otherwise
                if (isChecked) {
                    // Checkbox is checked, perform actions here
                    Log.d("Checkbox", "Checkbox is checked!")
                    clearAppData()
                } else {
                    // Checkbox is unchecked, perform actions here
                    Log.d("Checkbox", "Checkbox is unchecked!")
                }

            }
            btn.setOnClickListener {

                btn.isEnabled = false
                ProgressBarUtil.showProgressBar(this@LoginActivity)

                val run = {
                    Snackbar.make(it, "Check your Wifi Internet Connection", Snackbar.LENGTH_SHORT).show()
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                }
                var hand = Handler(Looper.getMainLooper())
                hand.postDelayed(run, 5000)
                passwordEt.isEnabled = false
                usernameEt.isEnabled = false
                if (!ProgressBarUtil.checkInternetConnection(this@LoginActivity)) {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                    Toast.makeText(
                        this@LoginActivity,
                        "Check your Internet Connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    hand.removeCallbacksAndMessages(null)
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    //return@setOnClickListener
                }
                else {
                    var isValidNumber=""
                    if (!number.isNullOrEmpty())
                   {
                       passwordEt.setText(number)
                   }
                    val uName=usernameEt.text.toString().trim()
                    val uPhone=passwordEt.text.toString().trim()
                    putData("user_name",uName)
                    putData("user_phone",uPhone)
                    if (uName.isNullOrEmpty())
                        usernameEt.error = "Please enter your name"
                    if (uPhone.isNotEmpty())
                    {
                        try {
                            isValidNumber=isValidAndAddCountryCode(uPhone)
                            if (isValidNumber.equals("Invalid"))
                            {
                                Toast.makeText(this@LoginActivity, "Invalid phone number", Toast.LENGTH_SHORT).show()
                            }
                            else{
                                mainRepository.login(uName, isValidNumber) { isDone, reason ->
                                    Log.d(TAG, "init: ${isValidNumber}")
                                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                                    hand.removeCallbacksAndMessages(null)
                                    passwordEt.isEnabled = true
                                    usernameEt.isEnabled = true
                                    btn.isEnabled = true
                                    if (!isDone) {
                                        Toast.makeText(this@LoginActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
                                    } else {
//                            val otpScreen=OTPScreen(this@LoginActivity)
//                            otpScreen.getOTP(uPhone){result,otp->
//                                // Force reCAPTCHA flow
///*   startActivity(Intent(this@LoginActivity, MainActivity::class.java
//                            ).apply { putExtra("uName",uPhone) })
//*/
//                            }
//                            Toast.makeText(this@LoginActivity, "clicked", Toast.LENGTH_SHORT).show()
//                            //start moving to our main activity

                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java
                                        ).apply { putExtra("username", uPhone) })

                                    }
                                }
                            }
                        }catch(e:Exception)
                        {
                            e.printStackTrace()
                        }

                    }



                }

            }
        }
    }


    private fun startMyService() {
        mainServiceRepository.startService("wifi", MainServiceActions.START_WIFI_SCAN.name)
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("image/*")
        }
        startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULTCODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "scanSuccess:onRequestPermissionsResult")
            }
        }else if (requestCode==1000)
        {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "scanSuccess:onRequestPermissionsResult")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            // Handle file chooser result
        }
    }



    private fun isPermissionGrand() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions granted, do nothing
        } else {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
        }
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mySMSBroadcastReceiver)
    }







    fun isValidAndAddCountryCode(phoneNumber: String): String {
        val pattern = "^\\+(?:\\d{1,3})?[789]{1}\\d{9}$"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(phoneNumber)

        return if (matcher.matches()) {
            phoneNumber
        } else {

            val formattedNumber = if (phoneNumber.length == 10) {
                "+91$phoneNumber"
            } else {

                "Invalid"
            }
            formattedNumber
        }
    }

    fun getData(key:String):String
    {
       return sharedPref.getString(key,"").toString()
    }

    fun putData(key:String,value:String){
        shEdit.putString(key, value)
        shEdit.apply()
    }

}


