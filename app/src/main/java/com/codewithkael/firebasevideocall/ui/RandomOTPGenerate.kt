package com.codewithkael.firebasevideocall.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.setViewFields

import kotlin.random.Random

class RandomOTPGenerate : AppCompatActivity()
{
    private var username: String? = null
    private var userphone: String? = null
    private var fetchedSimNo:String?=null
    private lateinit var timerId: TextView
    private lateinit var otp_verify_btn: Button
    private lateinit var get_otp_btn: Button
    private lateinit var otp_input: EditText
    private lateinit var countDownTimer: CountDownTimer
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_otpgenerate)
        init()
        otp_verify_btn.isEnabled=false//initially verify button is false
        otp_input.isEnabled=false//disable OTP input initially

        get_otp_btn.setOnClickListener {
            counterAndRandomNum()
            otp_input.setText("")
        }
        otp_verify_btn.setOnClickListener {
            otp_input.setText("")
            val resultIntent = Intent()
            resultIntent.putExtra(setViewFields.EXTRA_RESULT, "otp verified success")
            setResult(111, resultIntent)
            finish()
        }
    }

    private fun counterAndRandomNum()
    {
        timerId.visibility = View.VISIBLE
        countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                timerId.text = "Time remaining: $secondsRemaining seconds"
            }
            override fun onFinish() {
                val value = Random.nextInt(1000000).toString()
                    .padStart(6, '0') // Generates a 6-digit number
                otp_input.setText(value)
                timerId.text = "OTP Generated Successfully"
                if (value.isEmpty()) {
                    Toast.makeText(this@RandomOTPGenerate, "Failed to generate OTP", Toast.LENGTH_SHORT).show()
                }
                else
                {
                    otp_verify_btn.isEnabled = true //Enable the verify otp button after the otp generated
                    otp_input.isEnabled = true // Enable OTP input to allow copying or viewing
                    otp_input.isFocusable = false // Prevent user from typing in the OTP input field

                }
            }

        }.start()
    }



    override fun onBackPressed() {
        super.onBackPressed()
        otp_input.setText("")

    }
    private fun init()
    {
        //Extras from Main Activity
        username = intent.getStringExtra(setViewFields.EXTRA_USER_NAME)
        userphone = intent.getStringExtra(setViewFields.EXTRA_USER_PHONE)
        fetchedSimNo=intent.getStringExtra(setViewFields.EXTRA_SIM_NO)
        get_otp_btn = findViewById(R.id.get_otp_btn)
        otp_input = findViewById(R.id.otp_input)
        otp_verify_btn = findViewById(R.id.otp_verify_btn)
        timerId = findViewById(R.id.timerId)
    }

}