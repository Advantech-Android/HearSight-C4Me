package com.codewithkael.firebasevideocall.ui


import android.animation.ObjectAnimator
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.firebasevideocall.R
import kotlin.properties.Delegates


class SplashAcitivity : AppCompatActivity() {
    lateinit var sharedPref: SharedPreferences
    lateinit var shEdit: SharedPreferences.Editor
    var isLogin by Delegates.notNull<Boolean>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_acitivity)
        val progressBar =
            findViewById<ProgressBar>(R.id.progressBar) // Set up the rotation animation  // Duration in milliseconds // Infinite repeat  // Restart the animation// Start the animation
        val rotation = ObjectAnimator.ofFloat(progressBar, "rotation", 0f, 360f);
        rotation.setDuration(2000);
        rotation.repeatCount = ObjectAnimator.INFINITE;
        rotation.repeatMode = ObjectAnimator.RESTART;
        rotation.start();

        Handler(Looper.getMainLooper()).postDelayed(runHandler, 2000)
    }

    val runHandler = {
       /* if ( isVerificationStatusTrue()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
           finish()
        }*/
        startActivity(Intent(this, LoginActivity::class.java))
    }

    private fun isVerificationStatusTrue(): Boolean {
        sharedPref = applicationContext.getSharedPreferences("see_for_me", MODE_PRIVATE)
        return sharedPref.getBoolean("is_login", false)
    }

}