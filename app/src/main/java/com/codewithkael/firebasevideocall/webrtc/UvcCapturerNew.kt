package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.codewithkael.firebasevideocall.ui.CallActivity
import com.codewithkael.firebasevideocall.ui.CallActivity.Companion.uvcPreview
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.MultiCameraClient.ICamera
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.usb.USBMonitor.UsbControlBlock
import com.journeyapps.barcodescanner.camera.CameraInstance
import org.webrtc.CameraVideoCapturer
import org.webrtc.CameraVideoCapturer.CameraSwitchHandler
import org.webrtc.CapturerObserver
import org.webrtc.NV21Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame


import org.webrtc.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors
class UvcCapturerNew(
    context: Context,
    private val svVideoRender: SurfaceViewRenderer,
    private val eglBaseContext: EglBase.Context
) :
    CameraInstance(context), VideoCapturer, CameraVideoCapturer, CallActivity.UVCPreview {

    private val context: Context
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturerObserver: CapturerObserver? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var UVC_PREVIEW_WIDTH = 1280
    private var UVC_PREVIEW_HEIGHT = 720
    private var UVC_PREVIEW_FPS = 30
    private var uvcStrategy: CameraUvcStrategy? = null
    private var multiCameraClient: MultiCameraClient? = null
    private var isExecute = false
    private var isCheck = false
    private var isDetach = true
    var self: ICamera? = null
    var code: ICameraStateCallBack.State? = null
    var msg: String? = null


    companion object {
        private const val TAG = "###CallUvcCapturer"
    }

    init {
        Log.d(TAG, "UvcCapturer: ")
        this.context = context
        uvcPreview = this
        initializeUvcStrategy()
        initializeMultiCameraClient()
    }

    private fun initializeUvcStrategy() {
        if (uvcStrategy == null) {
            uvcStrategy = CameraUvcStrategy(context)
            uvcStrategy?.register()
        }
    }

    private fun initializeMultiCameraClient() {
        try {
            multiCameraClient = MultiCameraClient(context, object : IDeviceConnectCallBack {
                override fun onCancelDev(device: UsbDevice?) {
                    Log.d(TAG, "onCancelDev: ")
                }

                override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: UsbControlBlock?) {
                    Log.d(TAG, "onDisConnectDec: $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    resetFlags()
                }

                override fun onDetachDec(device: UsbDevice?) {
                    Log.d(TAG, "onDetachDec: isExecute--> $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    resetFlags()
                }

                override fun onConnectDev(device: UsbDevice?, ctrlBlock: UsbControlBlock?) {
                    Log.d(TAG, "onConnectDev: isExecute--> $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    if (device == null || ctrlBlock == null) {
                        Log.e(TAG, "onConnectDev: Device or CtrlBlock is null")
                        return
                    }
                    handleDeviceConnection(device)
                }

                override fun onAttachDev(device: UsbDevice?) {
                    Log.d(TAG, "onAttachDev:isExecute: $isExecute ==> isDetach:$isDetach")
                    if (device == null) {
                        Log.e(TAG, "onAttachDev: Device is null")
                        return
                    }
                    handleDeviceAttachment(device)
                }
            })
            multiCameraClient!!.register()
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetFlags() {
        isDetach = true
        isExecute = false
        isCheck = false
    }

    private fun handleDeviceConnection(device: UsbDevice) {
        if (!isDetach) {
            isDetach = true
            initializeUvcStrategy()
            createCapture(UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, UVC_PREVIEW_FPS)
        }
    }

    private fun handleDeviceAttachment(device: UsbDevice) {
        if (!isExecute) {
            isExecute = true
            multiCameraClient?.register()
            multiCameraClient?.requestPermission(device)
        }
        if (!isDetach) {
            isDetach = true
            initializeUvcStrategy()
            createCapture(UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, UVC_PREVIEW_FPS)
        }
    }

    override fun initialize(
        surfaceTextureHelper: SurfaceTextureHelper,
        context: Context,
        capturerObserver: CapturerObserver
    ) {
        Log.d(TAG, "initialize: ")
        this.surfaceTextureHelper = surfaceTextureHelper
        this.capturerObserver = capturerObserver
        try {
            svVideoRender.init(eglBaseContext, null)
        }catch (e:Exception){
            Log.d(TAG, "initialize: Error-->${e.message}")
        }
        svVideoRender.setMirror(true)
        svVideoRender.setEnableHardwareScaler(true)
    }

    override fun startCapture(i: Int, i1: Int, i2: Int) {
        Log.d(TAG, "startCapture: ")
        svVideoRender.post {
            createCapture(i, i1, i2)
        }
    }

    private fun createCapture(i: Int, i1: Int, i2: Int) {
        Log.d(TAG, "createCapture: ")
        if (uvcStrategy != null) {
            UVC_PREVIEW_WIDTH = i
            UVC_PREVIEW_HEIGHT = i1
            UVC_PREVIEW_FPS = i2
            uvcStrategy?.setZoom(0)
            uvcStrategy?.addPreviewDataCallBack(iPreviewDataCallBack)
            uvcStrategy?.startPreview(cameraRequest, svVideoRender.holder)
        }
        svVideoRender.holder.addCallback(svVideoRender)
    }

    @Throws(InterruptedException::class)
    override fun stopCapture() {
        Log.d(TAG, "stopCapture: ----------------------------------")
        if (uvcStrategy != null) {
            svVideoRender.release()
            uvcStrategy?.stopPreview()
            capturerObserver!!.onCapturerStarted(false)
            isExecute = false
        }
    }

    override fun changeCaptureFormat(i: Int, i1: Int, i2: Int) {}

    override fun dispose() {
        Log.d(TAG, "dispose: ******************************************")
        svVideoRender.release()
        surfaceTextureHelper!!.dispose()
        multiCameraClient!!.unRegister()
    }

    override fun isScreencast(): Boolean {
        return false
    }

    override fun switchCamera(p0: CameraSwitchHandler?) {}

    override fun switchCamera(p0: CameraSwitchHandler?, p1: String?) {}

    private val cameraRequest: CameraRequest
        get() = CameraRequest.Builder()
            .setFrontCamera(true)
            .setAspectRatioShow(true)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewWidth(UVC_PREVIEW_WIDTH)
            .setPreviewHeight(UVC_PREVIEW_HEIGHT)
            .create()

    val iPreviewDataCallBack = object : IPreviewDataCallBack {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {
            if (!isCheck) {
                Log.d(TAG, "onPreviewData() called with: data = $data, width = $width, height = $height, format = $format")
                isCheck = true
            }
            Log.d("--->***frameeeee", "onPreviewData: $data")
            val nv21Buffer = NV21Buffer(data, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, null)
            val frame = VideoFrame(nv21Buffer, 0, System.nanoTime())
            capturerObserver?.onFrameCaptured(frame)
            this@UvcCapturerNew.svVideoRender.onFrame(frame)
        }
    }

    override fun onUVCPreview(iCamera: ICamera, state: ICameraStateCallBack.State, s: String?, callFrom: String) {
        self = iCamera
        code = state
        msg = s
        Log.d(TAG, "onUVCPreview: isExecute=>$isExecute ,isDetach=$isDetach")

        if (!isExecute) {
            isExecute = true
            isDetach = false
            multiCameraClient?.register()
            multiCameraClient?.requestPermission(iCamera.device)
        } else {
            multiCameraClient?.register()
            multiCameraClient?.requestPermission(iCamera.device)
        }

        executor.execute {
            iCamera.addPreviewDataCallBack(iPreviewDataCallBack)
        }
    }

    override fun onCallEnd(msg: String) {
        Log.d(TAG, "onCallEnd: ************************************$msg")
        svVideoRender.holder.removeCallback(svVideoRender)
        self?.removePreviewDataCallBack(iPreviewDataCallBack)
        svVideoRender.removeCallbacks {
            Log.d(TAG, "onCallEnd: ---------")
        }
        if (uvcStrategy != null) {
            uvcStrategy?.stopPreview()
            uvcStrategy?.removePreviewDataCallBack(iPreviewDataCallBack)
            uvcStrategy?.unRegister()
        }

        multiCameraClient?.unRegister()
        isExecute = false
        isDetach = false
    }
}

