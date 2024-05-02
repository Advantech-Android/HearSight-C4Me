//package com.codewithkael.firebasevideocall.ui
//
//package com.codewithkael.firebasevideocall.ui
//
//
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import android.hardware.camera2.CameraAccessException
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CameraManager
//import android.hardware.usb.UsbDevice
//import android.hardware.usb.UsbDeviceConnection
//import android.hardware.usb.UsbManager
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.os.Parcelable
//import android.util.Log
//import android.view.SurfaceHolder
//import android.view.TextureView
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.activity.result.ActivityResultLauncher
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import com.codewithkael.firebasevideocall.databinding.ActivityHscallBinding
//import com.codewithkael.firebasevideocall.service.MainService
//import com.codewithkael.firebasevideocall.service.MainServiceRepository
//import com.codewithkael.firebasevideocall.utils.IntentAction
//import com.codewithkael.firebasevideocall.utils.TYPE_OF_MODE
//import com.codewithkael.firebasevideocall.utils.TYPE_OF_MODE.IS_HOST
//import com.codewithkael.firebasevideocall.utils.UserStatus
//import com.codewithkael.firebasevideocall.utils.convertToHumanTime
//import com.codewithkael.firebasevideocall.webrtc.HsRtcClient
//import com.herohan.uvcapp.CameraHelper
//import com.herohan.uvcapp.ICameraHelper
//import com.serenegiant.usb.IFrameCallback
//import com.serenegiant.usb.UVCCamera
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.webrtc.SurfaceViewRenderer
//import javax.inject.Inject
//
//
//private const val TAG = "===>>HSCallActivity"
//const val ACTION_USB_PERMISSION = "com.serenegiant.USB_PERMISSION."
//
//
//@AndroidEntryPoint
//class HSCallActivity : AppCompatActivity(), MainService.EndCallListener {
//    lateinit var binding: ActivityHscallBinding
//    private var target: String? = null
//    private var isVideoCall: Boolean = true
//    private var isCaller: Boolean = true
//    private var isMicrophoneMuted = false
//    private var isCameraMuted = false
//    private var isCameraOpen = false
//    private var isSpeakerMode = true
//    private var isScreenCasting = false
//    private var isUsingUSBCamera = false
//
//
//    /************************USB CAMERA********************************/
//    lateinit var cameraDevice: CameraDevice
//    lateinit var handler: Handler
//    lateinit var cameraManager: CameraManager
//
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
//    lateinit var webRTCClient: HsRtcClient
//
//
//    companion object inc {
//        var c = 1
//    }
//
//
//    @Inject
//    lateinit var serviceRepository: MainServiceRepository
//    private lateinit var requestScreenCaptureLauncher: ActivityResultLauncher<Intent>
//
//
//    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context, intent: Intent) {
//            val action = intent.action
//            Log.d(TAG, "onReceive: ------------${mUsbDevice?.deviceName}--------$action")
//            if (ACTION_USB_PERMISSION == action)
//            {
//                synchronized(this)
//                {
//                    if (mUsbDevice?.deviceName == null) {
//                        mUsbDevice =
//                            intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) as UsbDevice?
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
//                    } else {
//
//
//                    }
//                }
//            }
//        }
//    }
//    private var iCameraHelpCallBack: ICameraHelper.StateCallback =
//        object : ICameraHelper.StateCallback {
//            override fun onAttach(device: UsbDevice?) {
//                Log.d(TAG, "onAttach: call selectDevice-3")
//                mUsbDevice = device
//                selectDevice(device)
//            }
//
//
//            override fun onDeviceOpen(device: UsbDevice?, isFirstOpen: Boolean) {
//                Log.d(TAG, "onDeviceOpen: isFirstOpen =" + isFirstOpen)
//                Log.d(TAG, "onDeviceOpen: isCameraOpen =" + mCameraHelper?.isCameraOpened())
//                if (isFirstOpen) {
//                    mCameraHelper?.openCamera()
//                }
//
//
//            }
//
//
//            override fun onCameraOpen(device: UsbDevice?) {
//                Log.d(TAG, "onCameraOpen: ${device?.deviceName} times:${c++}")
//                if (mCameraHelper != null) {
//                    if (!isCameraOpen) {
//                        isCameraOpen = true
//                        webRTCClient.onCameraHelper(mCameraHelper!!)
//                        init()
//                    }
//
//
//                }
//
//
//
//
//
//
//            }
//
//
//            override fun onCameraClose(device: UsbDevice?) {
//                Log.d(TAG, "onCameraClose: ")
//
//
//
//
//            }
//
//
//            override fun onDeviceClose(device: UsbDevice?) {
//                Log.d(TAG, "onDeviceClose: ")
//            }
//
//
//            override fun onDetach(device: UsbDevice?) {
//                Log.d(TAG, "onDetach: ")
//            }
//
//
//            override fun onCancel(device: UsbDevice?) {
//                Log.d(TAG, "onCancel: ")
//            }
//
//
//        }
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityHscallBinding.inflate(layoutInflater)
//        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
//        setContentView(binding.root)
//        Log.d(TAG, "onCreate: ")
//        if (IS_HOST) {
//            initCameraHelper()
//        } else {
//            init()
//        }
//
//
//
//
//    }
//
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
//
//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        Toast.makeText(this, "onNewIntent", Toast.LENGTH_SHORT).show()
//        if (IS_HOST) {
//            if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
//                Log.d(
//                    TAG,
//                    "onNewIntent: ACTION_USB_DEVICE_ATTACHED-------call select device")
//                val mUsbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
//                selectDevice(mUsbDevice)
//            } else {
//                Log.d(TAG, "onNewIntent:${intent.action} ")
//            }
//        }
//    }
//
//
//    override fun onStart() {
//        super.onStart()
//        Log.d(TAG, "onStart: ")
//        Toast.makeText(this, "onStart", Toast.LENGTH_SHORT).show()
//        if (IS_HOST) {
//            initCameraHelper()
//            registerUSBReceiver()
//            getUSBDeviceList()
//        }
//
//
//    }
//
//
//    override fun onResume() {
//        super.onResume()
//        if (IS_HOST) {
//            if (mCameraHelper != null && usbManager != null && mUsbDevice != null) {
//                val hasPermision: Boolean = usbManager?.hasPermission(mUsbDevice) == true
//                if (!hasPermision) {
//                    usbManager?.requestPermission(mUsbDevice, mPermissionIntent)
//                } else {
//                    selectDevice(mUsbDevice)
//                    /* val connection: UsbDeviceConnection =
//                         usbManager?.openDevice(mUsbDevice) ?: return*/
//                }
//
//
//
//
//            } else {
//                Log.d(
//                    TAG,
//                    "onResume: onResume: mUsb =${mUsbDevice?.deviceName} mCameraHelper=$mCameraHelper usbManager=$usbManager"
//                )
//                Toast.makeText(
//                    this,
//                    "onResume: mUsb =${mUsbDevice?.deviceName}",
//                    Toast.LENGTH_SHORT
//                )
//                    .show()
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
//        if (IS_HOST) {
//            clearCameraHelper()
//            unregisterReceiver(usbReceiver)
//        }
//    }
//
//
//    private fun init() {
//
//
//        intent.getStringExtra(IntentAction.target)?.let {
//            Log.d(TAG, "init: $it")
//            this.target = it
//        } ?: kotlin.run { finish() }
//        isVideoCall = intent.getBooleanExtra(IntentAction.isVideoCall, true)
//        isCaller = intent.getBooleanExtra(IntentAction.isCaller, true)
//        Log.d(TAG, "init: isVideoCall=${isVideoCall} => isCaller=${isCaller}")
//        binding.apply {
//            callTitleTv.text = "In call with $target"
//            CoroutineScope(Dispatchers.IO).launch {
//                for (i in 0..3600) {
//                    delay(1000)
//                    withContext(Dispatchers.Main) {
//                        //convert this int to human readble time
//                        callTimerTv.text = i.convertToHumanTime()
//                    }
//                }
//            }
//            if (!isVideoCall) {
//                toggleCameraButton.isVisible = false
//                screenShareButton.isVisible = false
//                switchCameraButton.isVisible = false
//            }
//            MainService.localSurfaceView = localView
//            MainService.remoteSurfaceView = remoteView
//
//
//            serviceRepository.setupViews(isVideoCall, isCaller, target!!, uvc = true)
//
//
//        }
//    }
//
//
//    override fun onDestroy() {
//        super.onDestroy()
//        MainService.remoteSurfaceView?.release()
//        MainService.remoteSurfaceView = null
//
//
//        MainService.localSurfaceView?.release()
//        MainService.localSurfaceView = null
//
//
//    }
//
//
//    override fun onCallEnded() {
//        finish()
//    }
//
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
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (Build.VERSION.SDK_INT >= 34) {
//                ContextCompat.registerReceiver(
//                    this,
//                    usbReceiver,
//                    filter,
//                    ContextCompat.RECEIVER_VISIBLE_TO_INSTANT_APPS
//                )
//            } else {
//                registerReceiver(
//                    usbReceiver, filter,
//                    RECEIVER_VISIBLE_TO_INSTANT_APPS or RECEIVER_NOT_EXPORTED
//                )
//            }
//        } else {
//            registerReceiver(usbReceiver, filter)
//        }
//    }
//
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
//
//    private fun selectDevice(device: UsbDevice?) {
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
//}
