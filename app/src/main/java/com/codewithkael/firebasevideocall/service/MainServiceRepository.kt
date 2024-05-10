package com.codewithkael.firebasevideocall.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.codewithkael.firebasevideocall.utils.setViewFields
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {


    fun startService(username:String,intentAction:String){
        Thread{
            val intent = Intent(context, MainService::class.java)
            intent.putExtra(setViewFields.USER_NAME,username)
            //intent.action = MainServiceActions.START_SERVICE.name
            intent.action = intentAction
            startServiceIntent(intent)
        }.start()
    }

    private fun startServiceIntent(intent: Intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(intent)
        }else{
            context.startService(intent)
        }
    }
    fun setupViews(videoCall: Boolean, caller: Boolean, target: String, callerName: String) {
        val intent = Intent(context,MainService::class.java)
        intent.apply {
            action = MainServiceActions.SETUP_VIEWS.name
            putExtra(setViewFields.IS_VIDEO_CALL,videoCall)
            putExtra(setViewFields.TARGET,target)
            putExtra(setViewFields.IS_CALLER,caller)
            putExtra(setViewFields.CALLER_NAME,callerName)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.END_CALL.name
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.SWITCH_CAMERA.name
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO.name
        intent.putExtra(setViewFields.SHOULD_BE_MUTED,shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_VIDEO.name
        intent.putExtra(setViewFields.SHOULD_BE_MUTED,shouldBeMuted)
        startServiceIntent(intent)
    }

    fun toggleAudioDevice(type: String) {
        val intent = Intent(context, MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_AUDIO_DEVICE.name
        intent.putExtra(setViewFields.TYPE,type)
        startServiceIntent(intent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.TOGGLE_SCREEN_SHARE.name
        intent.putExtra(setViewFields.IS_STARTING,isStarting)
        startServiceIntent(intent)
    }

    fun stopService() {
        val intent = Intent(context,MainService::class.java)
        intent.action = MainServiceActions.STOP_SERVICE.name
        startServiceIntent(intent)
    }

}