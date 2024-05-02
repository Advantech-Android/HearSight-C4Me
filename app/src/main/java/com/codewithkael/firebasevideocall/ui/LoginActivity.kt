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
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.QRCode.WifiPasswordGenerated
import com.codewithkael.firebasevideocall.R
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
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.google.android.material.snackbar.Snackbar

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
    @Inject lateinit var mainRepository: MainRepository
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
      //  loginCredentials()
        modelDebug()

    }

    private fun loginCredentials(){
        views.apply {

            val usernameText = usernameEt.text.toString()
            val passwordText = passwordEt.text.toString()

            usernameEt.filters= arrayOf(InputFilter.LengthFilter(20))
            passwordEt.filters= arrayOf(InputFilter.LengthFilter(10),DigitsKeyListener.getInstance("0123456789"))

            usernameEt.setText(usernameText)
            passwordEt.setText(passwordText)
        }
    }
    private fun modelDebug() {
        views.apply {
            if (Build.BRAND!!.equals("oppo", true)) {
                //uvc.isUvc.value=true
                usernameEt.setText("Divya")
                passwordEt.setText("9843716886")
            } else {
                //Toast.makeText(this@LoginActivity, "UVC is Connected", Toast.LENGTH_SHORT).show()
                // uvc.isUvc.value=true
                usernameEt.setText("Pooja")
                passwordEt.setText("9994639839")
            }
        }
    }

    private fun init() {

        views.apply {
            btn.isEnabled=true
            btn.setOnClickListener {

                val usernameText = usernameEt.text.toString()
                val passwordText = passwordEt.text.toString()
//
                if (usernameText.isEmpty() || passwordText.isEmpty()) {
                    Toast.makeText(this@LoginActivity, "Enter both username and password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btn.isEnabled=false
                ProgressBarUtil.showProgressBar(this@LoginActivity)

                val run={
                    Snackbar.make(it,"Check your Wifi Internet Connection",Snackbar.LENGTH_SHORT).show()
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled=true
                }
                var hand=Handler(Looper.getMainLooper())
                hand.postDelayed(run, 5000)

                passwordEt.isEnabled = false
                usernameEt.isEnabled = false

                if (!ProgressBarUtil.checkInternetConnection(this@LoginActivity)) {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled=true
                    Toast.makeText(
                        this@LoginActivity,
                        "Check your Internet Connection",
                        Toast.LENGTH_SHORT
                    ).show()
                    hand.removeCallbacksAndMessages(null)
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    //return@setOnClickListener
                } else {
                    mainRepository.login(
                        usernameEt.text.toString(), passwordEt.text.toString()
                    ) { isDone, reason ->
                        ProgressBarUtil.hideProgressBar(this@LoginActivity)
                        hand.removeCallbacksAndMessages(null)
                        passwordEt.isEnabled = true
                        usernameEt.isEnabled = true
                        btn.isEnabled=true
                        if (!isDone) {

                            Toast.makeText(
                                this@LoginActivity,
                                "Something went wrong",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else
                        {
                            //start moving to our main activity
                            startActivity(Intent(
                                this@LoginActivity,
                                MainActivity::class.java
                            ).apply {

                                putExtra("username", passwordEt.text.toString())
                            })
                        }
                    }

                }

            }
        }
    }


    private fun startMyService() {
        mainServiceRepository.startService( "wifi", MainServiceActions.START_WIFI_SCAN.name)
    }
    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("image/*")
        }
        startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULTCODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_codeid -> {
                Log.d(TAG, "onOptionsItemSelected: ===>>>")
                isPermissionGrand()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    555
                )
                isCheckHotspot()

                if (wifiManager.isWifiEnabled )
                {
                    val wifiReceiver=WifiPasswordGenerated(this)
                    wifiReceiver.showQRDialog()
                }else{
                    Toast.makeText(this, "Please turn on Wifi", Toast.LENGTH_SHORT).show()
                }

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun isCheckHotspot(): Boolean {
        try {
            val npm = Class.forName("android.net.NetworkPolicyManager").getDeclaredMethod("from", Context::class.java).invoke(null, this)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            // Permissions granted, do nothing
        } else {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}


