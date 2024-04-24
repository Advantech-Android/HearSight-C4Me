package com.codewithkael.firebasevideocall.ui

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.Delegates

private const val TAG = "==>>LoginActivity"

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    object uvc {

        var isUvc = MutableLiveData<Boolean>(false)
    }

    private lateinit var views: ActivityLoginBinding
    @Inject
    lateinit var mainRepository: MainRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        Log.d(TAG, "onCreate: ${Build.BRAND}")
        views.apply {
            if (Build.BRAND!!.equals("samsung", true)) {
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
        init()
    }


    private fun init() {

        views.apply {
            btn.isEnabled=true
            btn.setOnClickListener {
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
//                                Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                            Toast.makeText(
                                this@LoginActivity,
                                "Something went wrong",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            //start moving to our main activity
                            startActivity(Intent(
                                    this@LoginActivity,
                                    MainActivity::class.java
                                ).apply {

                                    putExtra("username", usernameEt.text.toString())
                                })
                        }
                    }

                }

            }
        }
    }
}