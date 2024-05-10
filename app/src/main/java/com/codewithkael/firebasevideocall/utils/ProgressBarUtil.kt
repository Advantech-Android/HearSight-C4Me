@file:Suppress("DEPRECATION")

package com.codewithkael.firebasevideocall.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.firebasevideocall.R
import java.util.logging.Handler


object ProgressBarUtil

{
    private val handler=android.os.Handler(Looper.getMainLooper())
    private val progressBarDelayMillis:Long=10000//10 sconds dely millis

    fun showProgressBar(activity:AppCompatActivity) {
        activity.findViewById<ProgressBar>(R.id.progressBar)?.visibility=View.VISIBLE
    }
    fun hideProgressBar(activity: AppCompatActivity){
        activity.findViewById<ProgressBar>(R.id.progressBar)?.visibility=View.GONE
    }
    fun hidProgressbarWithDelay(activity: AppCompatActivity){
        handler.postDelayed({
            hideProgressBar(activity)
        }, progressBarDelayMillis)
    }
    fun checkInternetConnection(context: Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo=connectivityManager.activeNetworkInfo

        return networkInfo!=null && networkInfo.isConnected
    }
}