package com.codewithkael.firebasevideocall.ui

import WebQ
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository

import com.codewithkael.firebasevideocall.service.MainServiceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.os.Handler
import android.os.Looper
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.widget.Toast
import androidx.core.app.ActivityCompat


import androidx.lifecycle.MutableLiveData

import com.codewithkael.firebasevideocall.utils.LoginActivityFields
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.BOTH_USERNAME_PW
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.PASWORD_INVALID
import com.codewithkael.firebasevideocall.utils.MySMSBroadcastReceiver
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.codewithkael.firebasevideocall.utils.SnackBarUtils

private const val TAG = "***LoginActivity"
private const val STORAGE_PERMISSION_CODE = 123
private const val FILECHOOSER_RESULTCODE = 1

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    object uvc {

        var isUvc = MutableLiveData<Boolean>(false)
    }


    private lateinit var views: ActivityLoginBinding
    @Inject
    lateinit var mainRepository: MainRepository
    lateinit var webQ: WebQ
    lateinit var wifiManager: WifiManager
    lateinit var sharedPref: SharedPreferences
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

        wifiManager = applicationContext.getSystemService(
            Context.WIFI_SERVICE) as WifiManager
        init()
        //loginCredentials()
        modelDebug()

    }

    private fun loginCredentials(){
        views.apply {
            val userNameWatcher:  TextWatcher=object :TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().trimEnd().isEmpty())
                        usernameEt.error = BOTH_USERNAME_PW
                }

            }
            val passwordWatcher:  TextWatcher=object :TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                }

                override fun afterTextChanged(s: Editable?) {
                    Log.d(TAG, "afterTextChanged() called with: s = ${s.toString()}")
                    if (s.toString().trimEnd().length<13)
                        passwordEt.error = PASWORD_INVALID
                }

            }

            usernameEt.filters= arrayOf(InputFilter.LengthFilter(20))
            passwordEt.addTextChangedListener(passwordWatcher)
            usernameEt.addTextChangedListener(userNameWatcher)

            var passwordText = passwordEt.text.toString().trim().lowercase()
            val usernameText = usernameEt.text.toString().trimEnd()

            if(!passwordText.startsWith("+91")){
                passwordText="+91${passwordText}"
            }
            usernameEt.setText(usernameText)
            passwordEt.setText(passwordText)

        }
    }
    private fun modelDebug() {
        views.apply {
            if (Build.BRAND!!.equals("samsung", true)) {
                //uvc.isUvc.value=true
                usernameEt.setText("bheem")
                passwordEt.setText("+919994639839")
            }
            else
            {
                usernameEt.setText("chutki")
                passwordEt.setText("+919843716886")
            }
        }
    }

    private fun init(){
        setupButton()
    }
    private fun setupButton(){


        views.apply {
            btn.isEnabled=true
            btn.setOnClickListener {
                handleButtonClick()

            }
        }
    }


    private fun handleButtonClick() {
        views.apply {
            btn.isEnabled = true
            btn.setOnClickListener {

                btn.isEnabled = false
                val usernameText = usernameEt.text.toString().trim().lowercase().replace(" ","")
                val passwordText = passwordEt.text.toString().trim()

                if (usernameText.isEmpty()) {
                    SnackBarUtils.showSnackBar(views.root, BOTH_USERNAME_PW)
                    return@setOnClickListener
                }
                if (passwordText.toString().length<10||passwordText.isEmpty()) {
                    SnackBarUtils.showSnackBar(views.root, PASWORD_INVALID)
                    return@setOnClickListener
                }


                btn.isEnabled = false

                ProgressBarUtil.showProgressBar(this@LoginActivity)

                val run = {
                    // SnackBarUtils.showSnackBar(it, "Check your Wifi Internet Connection")
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                }
                var hand = Handler(Looper.getMainLooper())
                hand.postDelayed(run, 5000)

                passwordEt.isEnabled = false
                usernameEt.isEnabled = false

                if (!ProgressBarUtil.checkInternetConnection(this@LoginActivity))
                {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                    SnackBarUtils.showSnackBar(it, LoginActivityFields.CHECK_NET_CONNECTION)
                    hand.removeCallbacksAndMessages(null)
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)

                }
                else
                {
                    performLogin(usernameText,passwordText)
                }
            }
        }
    }

    private fun performLogin(usernameText: String, passwordText: String) {
        views.apply {
            mainRepository.login(
                usernameText,passwordText
            ) { isDone, reason ->
                Log.d(TAG, "Login attempt result: $isDone, Reason: $reason")
                ProgressBarUtil.hideProgressBar(this@LoginActivity)
                val hand = Handler(Looper.getMainLooper())
                hand.removeCallbacksAndMessages(null)
                passwordEt.isEnabled = true
                usernameEt.isEnabled = true
                btn.isEnabled = true
                if (!isDone) {
                    Log.d(TAG, "Login failed: $reason")
                    SnackBarUtils.showSnackBar(root, LoginActivityFields.UN_PW_INCORRECT)
                } else {
                    Log.d(TAG, "Login successful. Starting MainActivity.")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra("username", usernameText)
                    })

                }
            }
        }
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
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            // Handle file chooser result
        }
    }

}