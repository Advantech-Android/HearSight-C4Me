package com.codewithkael.firebasevideocall.ui

import android.Manifest.permission.READ_PHONE_NUMBERS
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.telephony.SubscriptionManager
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.setViewFields

import kotlin.random.Random

class RandomOTPGenerate : AppCompatActivity() {
    private lateinit var setmobileno: TextView
    private var username: String? = null
    private var userphone: String? = null
    private lateinit var timerId: TextView
    private lateinit var otp_verify_btn: Button
    private lateinit var get_otp_btn: Button
    private lateinit var otp_input: EditText
    private lateinit var countDownTimer: CountDownTimer
    lateinit var sharedPref: SharedPreferences
    lateinit var shEdit: SharedPreferences.Editor

    fun getData(key: String): String {
        return sharedPref.getString(key, "").toString()
    }

    fun putData(key: String, value: Any) {
        when (value) {
            is Int -> {
                shEdit.putInt(key, value)
            }

            is Boolean -> {
                shEdit.putBoolean(key, value)
            }

            is String -> {
                shEdit.putString(key, value)
            }
        }

        shEdit.apply()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//
//        // Check verification status from SharedPreferences
//        val isSharedPreferencesVerified = isVerificationStatusTrue()
//
//        // Check verification status from Firebase
//        isFirebaseVerificationTrue { isFirebaseVerified ->
//            // Navigate based on verification statuses
//            if (isSharedPreferencesVerified && isFirebaseVerified) {
//                // Both verification statuses are true, navigate to main activity
//                startActivity(Intent(this, MainActivity::class.java))
//                finish()
//            } else {
//                // At least one verification status is false, navigate to login activity
//                startActivity(Intent(this, LoginActivity::class.java))
//                finish()
//            }
//        }
        // Check verification status from SharedPreferences
        setContentView(R.layout.activity_random_otpgenerate)
        sharedPref = this.getSharedPreferences("see_for_me", MODE_PRIVATE)
        shEdit = sharedPref.edit()
        setmobileno = findViewById(R.id.setmobileno)//display the mobile number.
        get_otp_btn = findViewById(R.id.get_otp_btn)
        otp_input = findViewById(R.id.otp_input)
        otp_verify_btn = findViewById(R.id.otp_verify_btn)
        timerId = findViewById(R.id.timerId)

        //Extras from Main Activity
        username = intent.getStringExtra(setViewFields.USER_NAME)
        userphone = intent.getStringExtra(setViewFields.USER_PHONE)

        Log.d("TAG", "RandgetExtras==??:$username and $userphone ")
        shEdit.putString("user_name", username)
        shEdit.putString("user_phone", userphone)
        otp_verify_btn.isEnabled = false//initially verify button is false
        otp_input.isEnabled = false // disable OTP input initially

        // Request permissions at Runtime from user
        ActivityCompat.requestPermissions(this, arrayOf(READ_PHONE_NUMBERS), REQUEST_READ_PHONE_NUMBERS)

        get_otp_btn.setOnClickListener {
            if (setmobileno.text.toString().trim() == userphone?.trim()) {
                startOtpCountdown()
                otp_input.setText("")
            } else {
                startOtpCountdown()
                otp_input.setText("")
                Toast.makeText(this, "Please enter the correct Mobile number", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        otp_verify_btn.setOnClickListener {
            if (setmobileno.text.toString() == userphone)//if device sim number and typed number
            {
                startActivity(Intent(this@RandomOTPGenerate, MainActivity::class.java).apply {
                    putExtra(setViewFields.USER_NAME, username)
                    putExtra(setViewFields.USER_PHONE, userphone)
                })
                otp_input.setText("")//Clear otp input field
            } else {
                Toast.makeText(this@RandomOTPGenerate, "Login failed", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@RandomOTPGenerate, MainActivity::class.java).apply {
                    putExtra(setViewFields.USER_NAME, username)
                    putExtra(setViewFields.USER_PHONE, userphone)
                })
                otp_input.setText("")//Clear otp input field
            }
        }


    }

    //app can gracefully handle both granted and denied permissions to fetch the sim number
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_PHONE_NUMBERS) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, set the phone number
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    setPhoneNumber()
                else
                    setmobileno.text = "version not supported"//Checks if the device's Android version is TIRAMISU (API level 33) or higher.
            } else {
                // Permission denied, handle accordingly
                setmobileno.text = "Permission denied"
            }
        }
    }

    private fun startOtpCountdown() {
        timerId.visibility = View.VISIBLE
        countDownTimer = object : CountDownTimer(1000, 1000) {
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
                    Toast.makeText(
                        this@RandomOTPGenerate,
                        "Failed to generate OTP",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    otp_verify_btn.isEnabled = true //Enable the verify otp button after the otp generated
                    otp_input.isEnabled = true // Enable OTP input to allow copying or viewing
                    otp_input.isFocusable = false // Prevent user from typing in the OTP input field

                }
            }

        }.start()
    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setPhoneNumber() {
        //service allows you to access information about the telephony subscriptions (e.g., SIM cards) on a device.
        val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        //Compares the result to PackageManager.PERMISSION_GRANTED to determine if the permission has been granted.
        if (ActivityCompat.checkSelfPermission(this, READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED)
        { val stringPhoneNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID)
                    .trim()//fetch the primary SIM card in a dual-SIM device
            if (!stringPhoneNumber.isNullOrEmpty()) {
                setmobileno.text = "$stringPhoneNumber"//set the sim number
            } else {
                Toast.makeText(
                    this@RandomOTPGenerate, "Please insert the SIM card first", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Handle the case where permission is not granted
            Toast.makeText(
                this@RandomOTPGenerate,
                "Please allow the all permissions in your mobile",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        otp_input.setText("")

    }
//    private fun isVerificationStatusTrue():Boolean{
//        val sharedPreference=getSharedPreferences("verificationstatus",Context.MODE_PRIVATE)
//        return sharedPreference.getBoolean("isverified",false)
//    }

//    private fun isFirebaseVerificationTrue(callback:(Boolean)->Unit){
//        val firebaseUser = FirebaseAuth.getInstance().currentUser
//        val firebaseDatabase=FirebaseDatabase.getInstance()
//        firebaseUser?.uid?.let { uid ->
//            firebaseDatabase.reference.child("users").child(uid).child("verified")
//                .addListenerForSingleValueEvent(object : ValueEventListener{
//                    @SuppressLint("SuspiciousIndentation")
//                    override fun onDataChange(snapshot: DataSnapshot) {
//                        val isVerified=snapshot.getValue(Boolean::class.java)?:false
//                        callback(isVerified)
//                    }
//                    override fun onCancelled(error: DatabaseError) {
//                        callback(false)
//                    } })
//        }
//    }


    companion object {
        private const val REQUEST_READ_PHONE_NUMBERS = 1001//permission request code
    }
}
