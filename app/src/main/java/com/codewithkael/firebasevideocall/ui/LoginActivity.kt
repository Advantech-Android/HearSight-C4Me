package com.codewithkael.firebasevideocall.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
import com.thanosfisherman.wifiutils.WifiUtils
import com.thanosfisherman.wifiutils.wifiScan.ScanResultsListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates

private const val TAG = "==>>LoginActivity"
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    object uvc{

        var isUvc=MutableLiveData<Boolean>(false)
    }
    private lateinit var  views:ActivityLoginBinding
    @Inject lateinit var mainRepository: MainRepository
    var wifiManager: WifiManager? =null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
         wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        Log.d(TAG, "onCreate: ${Build.BRAND}")
        views.apply {
            if (Build.BRAND=="Itel"/*|| Build.BRAND!!.equals("Redmi",true)*/){

                usernameEt.setText("guest")
                passwordEt.setText("1234")
            }else
            {
                Toast.makeText(this@LoginActivity, "UVC is Connected", Toast.LENGTH_SHORT).show()
               // uvc.isUvc.value=true
                usernameEt.setText("host")
                passwordEt.setText("1234")
            }
        }
        init()
    }


    private fun init(){
        views.apply {
            btn.setOnClickListener {
                mainRepository.login(
                    usernameEt.text.toString(),passwordEt.text.toString()
                ){ isDone, reason ->
                    Log.d(TAG, "init:$reason ")
                    if (!isDone){
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    }else{
                        //start moving to our main activity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("username",usernameEt.text.toString())
                        }
                        )
                    }
                }
            }
        }

    }






}