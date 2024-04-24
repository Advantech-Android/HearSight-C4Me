    package com.codewithkael.firebasevideocall.ui

    import android.app.Activity
    import android.content.Context
    import android.content.Intent
    import android.media.projection.MediaProjectionManager
    import android.os.Bundle
    import android.os.CountDownTimer
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import android.widget.Toast
    import androidx.activity.result.ActivityResultLauncher
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.appcompat.app.AlertDialog
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.view.isGone
    import androidx.core.view.isVisible
    import com.codewithkael.firebasevideocall.R
    import com.codewithkael.firebasevideocall.databinding.ActivityCallBinding
    import com.codewithkael.firebasevideocall.repository.MainRepository
    import com.codewithkael.firebasevideocall.service.MainService
    import com.codewithkael.firebasevideocall.service.MainServiceRepository
    import com.codewithkael.firebasevideocall.utils.convertToHumanTime
    import com.codewithkael.firebasevideocall.webrtc.RTCAudioManager
    import dagger.hilt.android.AndroidEntryPoint
    import kotlinx.coroutines.*

    import javax.inject.Inject

    private const val TAG = "***>>CallActivity"

    @AndroidEntryPoint
    class CallActivity : AppCompatActivity(), MainService.EndCallListener {

        private var timer: Boolean=false
        private var isIncoming: String = ""
        private var target: String? = null
        private var isVideoCall: Boolean = true
        private var isCaller: Boolean = true

        private var isMicrophoneMuted = false
        private var isCameraMuted = false
        private var isSpeakerMode = true
        private var isScreenCasting = false

        @Inject
        lateinit var mp3Player: Mp3Ring

        @Inject
        lateinit var mainRepository: MainRepository

        @Inject
        lateinit var serviceRepository: MainServiceRepository
        private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>

        private lateinit var views: ActivityCallBinding

        override fun onStart() {
            super.onStart()
            requestScreenCaptureLauncher = registerForActivityResult(
                ActivityResultContracts
                    .StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data
                    //its time to give this intent to our service and service passes it to our webrtc client
                    MainService.screenPermissionIntent = intent
                    isScreenCasting = true
                    updateUiToScreenCaptureIsOn()
                    serviceRepository.toggleScreenShare(true)

                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            views = ActivityCallBinding.inflate(layoutInflater)
            setContentView(views.root)
            init()
        }

        private fun init() {
            intent.getStringExtra("target")?.let {
                this.target = it
            } ?: kotlin.run { finish() } // if there is not target extras it will finish the activity

            isVideoCall = intent.getBooleanExtra("isVideoCall", true)
            isCaller = intent.getBooleanExtra("isCaller", true)
            isIncoming = intent.getStringExtra("isIncoming").toString()
            timer = intent.getBooleanExtra("timer",false)

            Log.d(TAG, "init: isIncoming=$isIncoming")
            views.apply {
                callTitleTv.text = "In call with $target"
                if (!isCaller)startCallTimer()
                if (!isVideoCall) {
                    toggleCameraButton.isVisible = false
                    screenShareButton.isVisible = false
                    switchCameraButton.isVisible = false

                }
                MainService.remoteSurfaceView = remoteView
                MainService.localSurfaceView = localView
                serviceRepository.setupViews(isVideoCall, isCaller, target!!)

                endCallButton.setOnClickListener {
                    serviceRepository.sendEndCall()
                    //target- receiver
                    //sender-caller
                    mainRepository.setCallStatus(target=mainRepository.getUserName(), sender = target!!,"EndCall"){
                        mp3Player.stopMP3()
                    }
                }


                switchCameraButton.setOnClickListener {
                    serviceRepository.switchCamera()
                }
            }

            if (isIncoming.equals("Out", true)) {
                mp3Player.startMP3(false)
            }
            mainRepository.onObserveEndCall() { data,status ->
                Log.d(TAG, "CallAct -> onObserveEndCall: currentuser: ${mainRepository.getUserName()} target: ${target!!} status:$status")
                if (status == "EndCall"){
                        mp3Player.stopMP3()
                        mainRepository.setCallStatus(target=target!!, sender = mainRepository.getUserName(),""){
                            serviceRepository.sendEndCall()
                          //finish()
                        }
                }else if(status=="AcceptCall"){
//                    mainRepository.setCallStatus(target=target!!, sender = mainRepository.getUserName(),""){
//
//                    }
                    mp3Player.stopMP3()
                    startCallTimer()
                }
            }

            Handler(Looper.getMainLooper()).postDelayed({
                mp3Player.stopMP3()
            }, 20000)
            setupMicToggleClicked()
            setupCameraToggleClicked()
            setupToggleAudioDevice()
            setupScreenCasting()
            MainService.endCallListener = this
        }
         fun startCallTimer() {
            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0..3600) {
                    delay(1000)
                    withContext(Dispatchers.Main) {
                        //convert this int to human readable time
                        views.callTimerTv.text = i.convertToHumanTime()
                    }
                }
            }
        }
        private fun setupScreenCasting() {
            views.apply {
                screenShareButton.setOnClickListener {
                    if (!isScreenCasting) {
                        //we have to start casting
                        AlertDialog.Builder(this@CallActivity)
                            .setTitle("Screen Casting")
                            .setMessage("You sure to start casting ?")
                            .setPositiveButton("Yes") { dialog, _ ->
                                //start screen casting process
                                startScreenCapture()
                                dialog.dismiss()
                            }.setNegativeButton("No") { dialog, _ ->
                                dialog.dismiss()
                            }.create().show()
                    } else {
                        //we have to end screen casting
                        isScreenCasting = false
                        updateUiToScreenCaptureIsOff()
                        serviceRepository.toggleScreenShare(false)
                    }
                }

            }
        }

        private fun startScreenCapture() {
            val mediaProjectionManager = application.getSystemService(
                Context.MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            requestScreenCaptureLauncher.launch(captureIntent)

        }

        private fun updateUiToScreenCaptureIsOn() {
            views.apply {
                localView.isVisible = false
                switchCameraButton.isVisible = false
                toggleCameraButton.isVisible = false
                screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
            }

        }

        private fun updateUiToScreenCaptureIsOff() {
            views.apply {
                localView.isVisible = true
                switchCameraButton.isVisible = true
                toggleCameraButton.isVisible = true
                screenShareButton.setImageResource(R.drawable.ic_screen_share)
            }
        }

        private fun setupMicToggleClicked() {
            views.apply {
                toggleMicrophoneButton.setOnClickListener {
                    if (!isMicrophoneMuted) {
                        //we should mute our mic
                        //1. send a command to repository
                        serviceRepository.toggleAudio(true)
                        //2. update ui to mic is muted
                        toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
                    } else {
                        //we should set it back to normal
                        //1. send a command to repository to make it back to normal status
                        serviceRepository.toggleAudio(false)
                        //2. update ui
                        toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
                    }
                    isMicrophoneMuted = !isMicrophoneMuted
                }
            }
        }

        override fun onBackPressed() {
            super.onBackPressed()
            if (isIncoming.equals("Out", true)) {
                mp3Player.stopMP3()
            }
            serviceRepository.sendEndCall()
        }

        private fun setupToggleAudioDevice() {
            views.apply {
                toggleAudioDevice.setOnClickListener {
                    if (isSpeakerMode) {
                        //we should set it to earpiece mode
                        toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
                        //we should send a command to our service to switch between devices
                        serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)

                    } else {
                        //we should set it to speaker mode
                        toggleAudioDevice.setImageResource(R.drawable.ic_ear)
                        serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)

                    }
                    isSpeakerMode = !isSpeakerMode
                }

            }
        }

        private fun setupCameraToggleClicked() {
            views.apply {
                toggleCameraButton.setOnClickListener {
                    if (!isCameraMuted) {
                        serviceRepository.toggleVideo(true)
                        toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
                    } else {
                        serviceRepository.toggleVideo(false)
                        toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
                    }

                    isCameraMuted = !isCameraMuted
                }
            }
        }

        override fun onCallEnded() {
            finish()
        }

        override fun onDestroy() {
            super.onDestroy()

            MainService.remoteSurfaceView?.release()
            MainService.remoteSurfaceView = null

            MainService.localSurfaceView?.release()
            MainService.localSurfaceView = null

        }
    }