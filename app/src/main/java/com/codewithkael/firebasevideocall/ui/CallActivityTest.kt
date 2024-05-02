//
//package com.codewithkael.firebasevideocall.ui
//
//import android.app.Activity
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CameraManager
//import android.hardware.usb.UsbDevice
//import android.hardware.usb.UsbDeviceConnection
//import android.hardware.usb.UsbManager
//import android.media.projection.MediaProjectionManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AlertDialog
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import com.codewithkael.firebasevideocall.R
//import com.codewithkael.firebasevideocall.databinding.ActivityCallBinding
//import com.codewithkael.firebasevideocall.repository.MainRepository
//import com.codewithkael.firebasevideocall.service.MainService
//import com.codewithkael.firebasevideocall.service.MainServiceRepository
//import com.codewithkael.firebasevideocall.ui.LoginActivity.uvc.isUvc
//import com.codewithkael.firebasevideocall.utils.convertToHumanTime
//import com.codewithkael.firebasevideocall.webrtc.RTCAudioManager
//import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
//import com.herohan.uvcapp.CameraHelper
//import com.herohan.uvcapp.ICameraHelper
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class CallActivityTest : AppCompatActivity(), MainService.EndCallListener {
//    private var isCameraOpen=false
//    private  val TAG = "==>>CallActivity"
//    private var timer=false
//    private var target:String?=null
//    private var isIncoming:String?=""
//    private var callerName:String?=""
//    private var isVideoCall:Boolean= true
//    private var isCaller:Boolean = true
//
//    private var isMicrophoneMuted = false
//    private var isCameraMuted = false
//    private var isSpeakerMode = true
//    private var isScreenCasting = false
//
//
//
//    /************************USB CAMERA********************************/
//    lateinit var cameraDevice: CameraDevice
//    lateinit var handler: Handler
//    lateinit var cameraManager: CameraManager
//
//    private val DEFAULT_WIDTH = 1280
//    private val DEFAULT_HEIGHT = 720
//    private val ACTION_USB_PERMISSION: String = "com.serenegiant.USB_PERMISSION."
//    private var mUsbDevice: UsbDevice? = null
//    private var mCameraHelper: ICameraHelper? = null
//    private var usbManager: UsbManager? = null
//    private var mPermissionIntent: PendingIntent? = null
//
//
//    @Inject
//    lateinit var mp3Player: Mp3Ring
//
//    lateinit var usbDevice: UsbDevice
//
//    @Inject
//    lateinit var webRTCClient:WebRTCClient
//
//
//    @Inject
//    lateinit var mainRepository: MainRepository
//    @Inject lateinit var serviceRepository: MainServiceRepository
//    private lateinit var requestScreenCaptureLauncher:ActivityResultLauncher<Intent>
//
//    private lateinit var views:ActivityCallBinding
//
//    override fun onStart() {
//        super.onStart()
//        if(isUvc.value==true){
//            initCameraHelper()
//            registerUSBReceiver()
//            getUSBDeviceList()
//        }
//
//        requestScreenCaptureLauncher = registerForActivityResult(ActivityResultContracts
//            .StartActivityForResult()) { result ->
//            if (result.resultCode == Activity.RESULT_OK){
//                val intent = result.data
//                //its time to give this intent to our service and service passes it to our webrtc client
//                MainService.screenPermissionIntent = intent
//                isScreenCasting = true
//                updateUiToScreenCaptureIsOn()
//                serviceRepository.toggleScreenShare(true)
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        views = ActivityCallBinding.inflate(layoutInflater)
//        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
//
//        if (isUvc.value==true) {
//            initCameraHelper()
//        }
//            init()
//
//        setContentView(views.root)
//    }
//
//    private fun initCameraHelper() {
//        Log.d(TAG, "initCameraHelper:")
//        if (usbManager == null) {
//            usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
//        }
//        if (mCameraHelper == null) {
//            mCameraHelper = CameraHelper(ContextCompat.RECEIVER_EXPORTED)
//            mCameraHelper?.setStateCallback(iCameraHelpCallBack)
//        }
//    }
//
//    private fun registerUSBReceiver() {
//        mPermissionIntent = PendingIntent.getBroadcast(
//            this, 0, Intent(
//                ACTION_USB_PERMISSION
//            ), PendingIntent.FLAG_IMMUTABLE
//        )
//        val filter = IntentFilter(ACTION_USB_PERMISSION)
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
//        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
//        {
//            if (Build.VERSION.SDK_INT >= 34)
//            {
//                ContextCompat.registerReceiver(
//                    this,
//                    usbReceiver,
//                    filter,
//                    ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
//                )
//            }
//            else
//            {
//                ContextCompat.registerReceiver(
//                    this,
//                    usbReceiver,
//                    filter,
//                    ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS or ContextCompat.RECEIVER_NOT_EXPORTED
//                )
//            }
//        } else {
//            registerReceiver(usbReceiver, filter)
//        }
//    }
//
//    private fun getUSBDeviceList() {
//
//
//        if (usbManager != null) {
//            val deviceList: HashMap<String, UsbDevice>? = usbManager?.deviceList
//            for (usbDevice in deviceList?.values!!) {
//                // Log.d(TAG, "getUSBDeviceList: ${usbDevice.deviceName}")
//                mUsbDevice = usbDevice
//                usbManager?.requestPermission(usbDevice, mPermissionIntent)
//            }
//            if (mUsbDevice != null) {
//                val hasPermision: Boolean = usbManager?.hasPermission(mUsbDevice) == true
//                if (hasPermision) {
//                    Log.d(TAG, "getUSBDeviceList: hasPermision = $hasPermision call selectdevice")
//                    selectDevice(mUsbDevice)
//                    val connection: UsbDeviceConnection =
//                        usbManager?.openDevice(mUsbDevice) ?: return
//                } else {
//                    if (usbManager?.hasPermission(mUsbDevice) == false) {
//                        // Request permission from the user
//                        usbManager?.requestPermission(mUsbDevice, mPermissionIntent)
//                    }
//                }
//                /*    val connection: UsbDeviceConnection =
//                        usbManager?.openDevice(mUsbDevice) ?: return*/
//
//
//            } else {
//                Toast.makeText(
//                    this,
//                    "getUSBDeviceList: mUsbDevice =$mUsbDevice",
//                    Toast.LENGTH_SHORT
//                ).show()
//
//
//            }
//        }
//    }
//
//    private var iCameraHelpCallBack:ICameraHelper.StateCallback=object :ICameraHelper.StateCallback{
//        override fun onAttach(device: UsbDevice?) {
//            mUsbDevice=device
//            selectDevice(device)
//        }
//
//        override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
//       Log.d(TAG,"is device open? =$isFirstOpen")
//                mCameraHelper?.openCamera()
//
//        }
//        override fun onCameraOpen(device: UsbDevice?) {
//            //Log.d(TAG, "onCameraOpen: ${device?.deviceName} times:${HSCallActivity.c++}")
//            Toast.makeText(this@CallActivityTest,    "CameraOpened", Toast.LENGTH_SHORT).show()
//          /*  if (mCameraHelper != null) {
//                if (!isCameraOpen) {
//                    isCameraOpen = true
//                    webRTCClient.onCameraHelper(mCameraHelper!!)
//
//                }
//
//
//            }*/
//
//        }
//
//        override fun onCameraClose(device: UsbDevice?) {
//            Log.d(TAG,"onCameraClose")
//        }
//
//        override fun onDeviceClose(device: UsbDevice?) {
//            Log.d(TAG,"onDeviceClose")
//        }
//
//        override fun onDetach(device: UsbDevice?) {
//            Log.d(TAG,"onDetach")
//        }
//
//        override fun onCancel(device: UsbDevice?) {
//            Log.d(TAG,"onCancel")
//        }
//
//    }
//
//
//
//    override fun onResume() {
//        super.onResume()
//        if (isUvc.value==true) {
//            if (mCameraHelper != null && usbManager != null && mUsbDevice != null) {
//                val hasPermision: Boolean = usbManager?.hasPermission(mUsbDevice) == true
//                if (!hasPermision) {
//                    usbManager?.requestPermission(mUsbDevice, mPermissionIntent)
//                } else {
//                    selectDevice(mUsbDevice)
//                    /* val connection: UsbDeviceConnection =
//                         usbManager?.openDevice(mUsbDevice) ?: return*/
//                }
//            } else {
//                Log.d(
//                    TAG,
//                    "onResume: onResume: mUsb =${mUsbDevice?.deviceName} mCameraHelper=$mCameraHelper usbManager=$usbManager"
//                )
//                Toast.makeText(
//                    this,
//                    "onResume: Please Connect UVC Camera Properly",
//                    Toast.LENGTH_SHORT
//                )
//                    .show()
//                 serviceRepository.sendEndCall()
//            }
//        }
//
//
//    }
//
//
//    override fun onStop() {
//        super.onStop()
//        Toast.makeText(this, "onStop", Toast.LENGTH_SHORT).show()
//        if (isUvc.value==true) {
//            clearCameraHelper()
//            unregisterReceiver(usbReceiver)
//        }
//    }
//
//    private fun clearCameraHelper() {
//        Log.d(TAG, "clearCameraHelper:")
//        if (mCameraHelper != null) {
//            mCameraHelper!!.release()
//            mCameraHelper = null
//        }
//    }
//
//
//    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//
//            Log.d(TAG, "onReceive: ------------${mUsbDevice?.deviceName}--------$action")
//            if (ACTION_USB_PERMISSION == action) {
//                synchronized(this) {
//                    if (mUsbDevice?.deviceName == null) {
//                        mUsbDevice =
//                            (intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) as UsbDevice?)!!
//                    }
//
//
//                    if (intent.getBooleanExtra(
//                            UsbManager.EXTRA_PERMISSION_GRANTED,
//                            false
//                        )
//                    ) {
//                        if (mUsbDevice != null) {
//                            selectDevice(mUsbDevice)
//                            Log.d(TAG, "onReceive: call select device-1")
//                        } else Log.d(TAG, "EXTRA_PERMISSION_GRANTED: device not found-1")
//                    } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
//                        Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED: device attached")
//                        if (mUsbDevice != null) {
//                            Log.d(TAG, "onReceive: call select device-2")
//                            selectDevice(mUsbDevice)
//                        } else Log.d(TAG, "ACTION_USB_DEVICE_ATTACHED : device not found-2")
//
//
//                    }
//                    else
//                    {
//
//
//                    }
//                }
//            }
//        }
//    }
//
//
//
//
// private fun selectDevice(device: UsbDevice?) {
//        if (device != null && mCameraHelper != null) {
//            mUsbDevice = device
//            mCameraHelper?.selectDevice(device)
//            Log.v(
//                TAG,
//                "selectDevice:device=" + device.deviceName
//            )
//
//
//        }
//    }
//
//    private fun init(){
//        intent.getStringExtra("target")?.let {
//            this.target = it
//        }?: kotlin.run {
//            finish()
//        }
//
//        isVideoCall = intent.getBooleanExtra("isVideoCall",true)
//        isCaller = intent.getBooleanExtra("isCaller",true)
//        isIncoming = intent.getStringExtra("isIncoming").toString()
//        timer = intent.getBooleanExtra("timer",false)
//        callerName = intent.getStringExtra("callerName").toString()
//        Log.d(TAG, "init: isIncoming=$isIncoming")
//        views.apply {
//            callTitleTv.text = "In call with $target"
//            if (!isCaller)startCallTimer()
//
//            if (!isVideoCall){
//                toggleCameraButton.isVisible = false
//                screenShareButton.isVisible = false
//                switchCameraButton.isVisible = false
//
//            }
//            MainService.remoteSurfaceView = remoteView
//            MainService.localSurfaceView = localView
//            serviceRepository.setupViews(isVideoCall,isCaller,target!!, callerName!!)
//
//            endCallButton.setOnClickListener {
//                serviceRepository.sendEndCall()
//                //target- receiver
//                //sender-caller
//                Log.d(TAG, "init: EndCall => target :${mainRepository.getUserPhone()}")
//                mainRepository.setCallStatus(target=mainRepository.getUserPhone(), sender = target!!,"EndCall"){
//
//                    mp3Player.stopMP3()
//                }
//            }
//
//            switchCameraButton.setOnClickListener {
//                serviceRepository.switchCamera()
//            }
//        }
//
//        if (isIncoming.equals("Out", true)) {
//            mp3Player.startMP3(false)
//        }
//        mainRepository.onObserveEndCall() { data,status ->
//            Log.d(TAG, "CallAct -> onObserveEndCall: currentuser: ${mainRepository.getUserPhone()} target: ${target!!} status:$status")
//            if (status == "EndCall"){
//                mp3Player.stopMP3()
//                mainRepository.setCallStatus(target=target!!, sender = mainRepository.getUserPhone(),""){
//                    serviceRepository.sendEndCall()
//                    //finish()
//                }
//            }else if(status=="AcceptCall"){
////                    mainRepository.setCallStatus(target=target!!, sender = mainRepository.getUserPhone(),""){
////
////                    }
//                mp3Player.stopMP3()
//                startCallTimer()
//            }
//        }
//
//        Handler(Looper.getMainLooper()).postDelayed({
//            mp3Player.stopMP3()
//        }, 20000)
//        setupMicToggleClicked()
//        setupCameraToggleClicked()
//        setupToggleAudioDevice()
//        setupScreenCasting()
//        MainService.endCallListener = this
//    }
//
//    fun startCallTimer() {
//        CoroutineScope(Dispatchers.IO).launch {
//            for (i in 0..3600) {
//                delay(1000)
//                withContext(Dispatchers.Main) {
//                    //convert this int to human readable time
//                    views.callTimerTv.text = i.convertToHumanTime()
//                }
//            }
//        }
//    }
//
//    private fun setupScreenCasting() {
//        views.apply {
//            screenShareButton.setOnClickListener {
//               if (!isScreenCasting){
//                   //we have to start casting
//                   AlertDialog.Builder(this@CallActivityTest)
//                       .setTitle("Screen Casting")
//                       .setMessage("You sure to start casting ?")
//                       .setPositiveButton("Yes"){dialog,_ ->
//                           //start screen casting process
//                           startScreenCapture()
//                           dialog.dismiss()
//                       }.setNegativeButton("No") {dialog,_ ->
//                           dialog.dismiss()
//                       }.create().show()
//               }else{
//                   //we have to end screen casting
//                   isScreenCasting = false
//                   updateUiToScreenCaptureIsOff()
//                   serviceRepository.toggleScreenShare(false)
//               }
//            }
//
//        }
//    }
//
//
//    private fun startScreenCapture() {
//        val mediaProjectionManager = application.getSystemService(
//            Context.MEDIA_PROJECTION_SERVICE
//        ) as MediaProjectionManager
//
//        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
//        requestScreenCaptureLauncher.launch(captureIntent)
//
//    }
//
//    private fun updateUiToScreenCaptureIsOn(){
//        views.apply {
//            localView.isVisible = false
//            switchCameraButton.isVisible = false
//            toggleCameraButton.isVisible = false
//            screenShareButton.setImageResource(R.drawable.ic_stop_screen_share)
//        }
//
//    }
//    private fun updateUiToScreenCaptureIsOff() {
//        views.apply {
//            localView.isVisible = true
//            switchCameraButton.isVisible = true
//            toggleCameraButton.isVisible = true
//            screenShareButton.setImageResource(R.drawable.ic_screen_share)
//        }
//    }
//    private fun setupMicToggleClicked(){
//        views.apply {
//            toggleMicrophoneButton.setOnClickListener {
//                if (!isMicrophoneMuted){
//                    //we should mute our mic
//                    //1. send a command to repository
//                    serviceRepository.toggleAudio(true)
//                    //2. update ui to mic is muted
//                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_on)
//                }else{
//                    //we should set it back to normal
//                    //1. send a command to repository to make it back to normal status
//                    serviceRepository.toggleAudio(false)
//                    //2. update ui
//                    toggleMicrophoneButton.setImageResource(R.drawable.ic_mic_off)
//                }
//                isMicrophoneMuted = !isMicrophoneMuted
//            }
//        }
//    }
//
//    override fun onBackPressed() {
//        super.onBackPressed()
//        if (isIncoming.equals("Out", true)) {
//            mp3Player.stopMP3()
//        }
//        serviceRepository.sendEndCall()
//    }
//
//
//    private fun setupToggleAudioDevice(){
//        views.apply {
//            toggleAudioDevice.setOnClickListener {
//                if (isSpeakerMode){
//                    Log.d("ic_speaker", "setupToggleAudioDevice: $isSpeakerMode")
//                    //we should set it to earpiece mode
//                    toggleAudioDevice.setImageResource(R.drawable.ic_speaker)
//                    //we should send a command to our service to switch between devices
//                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.EARPIECE.name)
//
//                }else{
//                    Log.d("ic_speaker", "setupToggleAudioDevice: $isSpeakerMode")
//                    //we should set it to speaker mode
//                    toggleAudioDevice.setImageResource(R.drawable.ic_ear)
//                    serviceRepository.toggleAudioDevice(RTCAudioManager.AudioDevice.SPEAKER_PHONE.name)
//
//                }
//                isSpeakerMode = !isSpeakerMode
//            }
//
//        }
//    }
//
//    private fun setupCameraToggleClicked(){
//        views.apply {
//            toggleCameraButton.setOnClickListener {
//                if (!isCameraMuted){
//                    serviceRepository.toggleVideo(true)
//                    toggleCameraButton.setImageResource(R.drawable.ic_camera_on)
//                }else{
//                    serviceRepository.toggleVideo(false)
//                    toggleCameraButton.setImageResource(R.drawable.ic_camera_off)
//                }
//
//                isCameraMuted = !isCameraMuted
//            }
//        }
//    }
//
//    override fun onCallEnded() {
//        finish()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        MainService.remoteSurfaceView?.release()
//        MainService.remoteSurfaceView = null
//        MainService.localSurfaceView?.release()
//        MainService.localSurfaceView =null
//
//    }
//}
//
//
//
