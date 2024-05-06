package com.codewithkael.firebasevideocall.ui


import WebQ
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.service.MainServiceActions
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.text.method.DigitsKeyListener

import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.utils.LoginActivityFields
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.BOTH_USERNAME_PW
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.PASWORD_INVALID
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

    private lateinit var webView1: WebView
    private lateinit var views: ActivityLoginBinding
    @Inject
    lateinit var mainRepository: MainRepository
    lateinit var webQ: WebQ
    lateinit var wifiManager: WifiManager

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        init()
      loginCredentials()
       // modelDebug()

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

            var passwordText = passwordEt.text.toString().trim()
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
                    SnackBarUtils.showSnackBar(views.root, LoginActivityFields.BOTH_USERNAME_PW)
                    return@setOnClickListener
                }
                if (passwordText.toString().length<10||passwordText.isEmpty()) {
                    SnackBarUtils.showSnackBar(views.root, LoginActivityFields.PASWORD_INVALID)
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

                if (!ProgressBarUtil.checkInternetConnection(this@LoginActivity)) {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                    SnackBarUtils.showSnackBar(it, LoginActivityFields.CHECK_NET_CONNECTION)
                    hand.removeCallbacksAndMessages(null)
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    //return@setOnClickListener
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
                ProgressBarUtil.hideProgressBar(this@LoginActivity)
                val hand = Handler(Looper.getMainLooper())
                hand.removeCallbacksAndMessages(null)
                passwordEt.isEnabled = true
                usernameEt.isEnabled = true
                btn.isEnabled = true
                if (!isDone) {
                    SnackBarUtils.showSnackBar(root, LoginActivityFields.UN_PW_INCORRECT)
                } else {
                    startActivity(Intent(
                        this@LoginActivity,
                        MainActivity::class.java
                    ).apply {
                        putExtra("username", usernameText)
                    })
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
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            // Handle file chooser result
        }
    }


    private fun isCheckHotspot(): Boolean {
        try {
            val npm = Class.forName("android.net.NetworkPolicyManager")
                .getDeclaredMethod("from", Context::class.java).invoke(null, this)
            val policies = npm.javaClass.getDeclaredMethod("getNetworkPolicies").invoke(npm)

            if (policies != null) {
                val policyArray = policies as Array<Any>
                for (policy in policyArray) {
                    val isHotspotEnabled =
                        policy.javaClass.getDeclaredMethod("isMetered", *arrayOfNulls(0))
                            .invoke(policy) as Boolean
                    if (isHotspotEnabled) {
                        return true
                    } else {
                        return false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
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

    override fun onDestroy() {
        super.onDestroy()
    }

}


