package com.codewithkael.firebasevideocall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import com.codewithkael.firebasevideocall.ui.CloseActivity
import com.codewithkael.firebasevideocall.ui.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private const val TAG = "==>>MainServiceReceiver"


@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject lateinit var serviceRepository: MainServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {

        Log.d(TAG, "onReceive: ${intent?.action}")

        if (intent?.action == "ACTION_EXIT"){
            //we want to exit the whole application
            serviceRepository.stopService()
            context?.startActivity(Intent(context,CloseActivity::class.java))

        }
        if(intent?.action==Intent.ACTION_BATTERY_CHANGED){
            val temprature=intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)/10.0
            LoginActivity.tempLiveData.value=temprature.toString()
        }

    }
}