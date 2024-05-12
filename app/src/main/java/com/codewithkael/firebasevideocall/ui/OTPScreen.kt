package com.codewithkael.firebasevideocall.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import com.codewithkael.firebasevideocall.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class OTPScreen(private val context: Context) {
    private val TAG = "OTPScreen"
    private lateinit var auth: FirebaseAuth
    private lateinit var otpInput: TextView
    private lateinit var timerText: TextView
    private lateinit var otpButton: Button
    private lateinit var otpPhoneNumber: String
    private lateinit var resultCallback: (Boolean, String) -> Unit
    private lateinit var verificationId:String
    private lateinit var smsCode:String


    fun getOTP(phoneNumber: String)
    {

        val otpDialog = Dialog(context)
        otpDialog.setContentView(R.layout.otp_verify_layout)
        otpDialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        otpInput = otpDialog.findViewById(R.id.otp_input)
        timerText = otpDialog.findViewById<TextView>(R.id.timerId)
        otpButton = otpDialog.findViewById<Button>(R.id.otp_btn)
        otpButton.isEnabled=false
        otpDialog.show()
        setTimer(timerText)


        callOtpServer()


        otpButton.setOnClickListener {
            smsCode=otpInput.text.toString()
            if (smsCode.isNullOrEmpty())
            {
                Toast.makeText(context, "Please enter the OTP...", Toast.LENGTH_SHORT).show()
            }
            verifyOTP(verificationId, smsCode) }
    }

    private fun callOtpServer() {
        otpButton.text = "Verify"

        auth = FirebaseAuth.getInstance()
        auth.setLanguageCode("en")

        if (auth.currentUser != null) {
            auth.signOut()
        }

        val fullPhoneNumber = otpPhoneNumber
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(TAG, "onVerificationCompleted: ${credential.smsCode}")
                    otpInput.text = credential.smsCode.toString()
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.d(TAG, "onVerificationFailed: ")
                }

                override fun onCodeSent(mverificationId: String, token: PhoneAuthProvider.ForceResendingToken) {

                    Log.d(TAG, "onCodeSent: forceResendingToken\t$token")
                    otpButton.isEnabled=true
                    verificationId=mverificationId
                    Log.d(TAG, "onCodeSent: verificationId\t$verificationId ")

                }
            })

        PhoneAuthProvider.verifyPhoneNumber(options.build())

    }

    private fun setTimer(timerText: TextView) {
        timerText.visibility = View.VISIBLE
        val timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {

                timerText.text = "Resend"
                timerText.setTextColor(Color.parseColor("#213C51"))
                timerText.setOnClickListener {
                    if (otpPhoneNumber.isNotEmpty()) {
                        callOtpServer()
                    }
                }
            }
        }
        timer.start()
    }


    private fun verifyOTP(verificationId: String, smsCode: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    context.startActivity(Intent(context, MainActivity::class.java).apply { putExtra("username", otpPhoneNumber) })
                    resultCallback(true, credential.smsCode!!)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(context, "Please enter the valid OTP", Toast.LENGTH_SHORT).show()
                    resultCallback(false, "")
                    context.startActivity(Intent(context, MainActivity::class.java).apply { putExtra("username", otpPhoneNumber) })
                    resultCallback(true, credential.smsCode!!)
                }
            }
    }
}
