package com.codewithkael.firebasevideocall.ui

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.OTPFields
import com.codewithkael.firebasevideocall.utils.SnackBarUtils
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit


class OTPScreen(private val context: Context) {
    var TAG="===>OTPScreen"
    lateinit var storedVerificationId: String
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    lateinit var auth: FirebaseAuth
    lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks


    fun getOTP(phoneNumber: String)
    {
        val otpDialog = Dialog(context)
        otpDialog.setContentView(R.layout.otp_verify_layout)
        otpDialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        val otpInput = otpDialog.findViewById<TextView>(R.id.otp_input)
        val timerText = otpDialog.findViewById<TextView>(R.id.timerId)
        val otpButton = otpDialog.findViewById<Button>(R.id.otp_btn)
        otpDialog.show()
        otpButton.setOnClickListener {
            setTimer(timerText)
            callOtpServer(otpButton, timerText, otpInput,phoneNumber)
        }
    }


    private fun callOtpServer(otpButton: Button, timerText: TextView,otp:TextView, phoneNumber: String) {
        otpButton.text = "Please wait..."
        otpButton.isEnabled = false
        auth=FirebaseAuth.getInstance()

        callbacks=object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithPhoneAuthCredential(credential,otp)
            }

            override fun onVerificationFailed(error: FirebaseException) {
                Log.d(TAG, "onVerificationFailed: ${error}")
                SnackBarUtils.showSnackBar(otpButton,"${error}")

            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                super.onCodeSent(verificationId, token)
                resendToken = token

            }
        }

        val full_phoneNumber="+91"+phoneNumber
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(full_phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(callbacks)
            .build()
        Log.d(TAG, "callOtpServer: ${phoneNumber}")
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private fun setTimer(timerText: TextView) {
        timerText.visibility = View.VISIBLE
        val timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val mtimer = millisUntilFinished / 1000
                timerText.text = mtimer.toString()
            }

            override fun onFinish() {
                timerText.text="Please try again"
            }
        }
        timer.start()
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, otp: TextView) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    otp.text=credential.smsCode.toString()
                    context.startActivity(Intent(context, MainActivity::class.java))
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    SnackBarUtils.showSnackBar(otp,OTPFields.AUTH_FAIL)
                }
            }
    }

}