package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.util.Log
import android.widget.Toast
import com.codewithkael.firebasevideocall.service.MainService
import com.codewithkael.firebasevideocall.ui.CallActivity
import com.codewithkael.firebasevideocall.ui.CallActivity.Companion.uvcPreview
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.MultiCameraClient.ICamera
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.CameraUvcStrategy
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.RenderManager.CameraSurfaceTextureListener
import com.jiangdg.usb.USBMonitor.UsbControlBlock
import com.jiangdg.uvc.UVCCamera
import com.journeyapps.barcodescanner.camera.CameraInstance
import org.webrtc.CameraVideoCapturer
import org.webrtc.CameraVideoCapturer.CameraSwitchHandler
import org.webrtc.CapturerObserver
import org.webrtc.NV21Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UvcCapturerNew(context: Context, svVideoRender: SurfaceViewRenderer) :
    CameraInstance(context), VideoCapturer, CameraVideoCapturer,
    CallActivity.UVCPreview {
    private lateinit var isCalledFrom: String
    private val context: Context
    private var svVideoRender: SurfaceViewRenderer
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturerObserver: CapturerObserver? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var UVC_PREVIEW_WIDTH = 1280
    private var UVC_PREVIEW_HEIGHT = 720
    private var UVC_PREVIEW_FPS = 30
    private var mUvcStrategy: CameraUvcStrategy? = null // From Jiang Dongguo's AUSBC library
    private var uvcCamera: CameraUVC? = null

    var self: ICamera? = null
    var code: ICameraStateCallBack.State? = null
    var msg: String? = null
    var multicam: MultiCameraClient? = null
    var isExecute = false
    var isCheck = false
    var isDetach = true

    companion object {
        private const val TAG = "###CallUvcCapturer"

    }
fun createUVCInstance(){
    if(mUvcStrategy==null)
    {
        mUvcStrategy = CameraUvcStrategy(context)
    }

    mUvcStrategy?.register()
}
    init {

        Log.d(TAG, "UvcCapturer: ")
        this.context = context
        this.svVideoRender = svVideoRender
        uvcPreview = this
        createUVCInstance()

        try {


            multicam = MultiCameraClient(context, object : IDeviceConnectCallBack {
                /**
                 * @param device
                 */
                override fun onCancelDev(device: UsbDevice?) {
                    Log.d(TAG, "onCancelDev: ")
                }

                /**
                 * @param device
                 * @param ctrlBlock
                 */
                override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: UsbControlBlock?) {
                    Log.d(TAG, "onDisConnectDec: $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    isDetach=false
                    isExecute = false
                    isCheck=false

                    Log.d(TAG, "onDisConnectDec: $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    isDetach = true
                    isExecute = false
                    isCheck = false
                    uvcCamera?.captureStreamStop()
                    uvcCamera?.closeCamera()
                    uvcCamera = null
                }

                /**
                 * @param device
                 */
                override fun onDetachDec(device: UsbDevice?) {


                    Log.d(TAG, "onDetachDec: isExecute--> $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")
                    isDetach = true
                    isExecute = false
                    isCheck = false
                    uvcCamera?.captureStreamStop()
                    uvcCamera?.closeCamera()
                    uvcCamera = null
                }

                /**
                 * @param device
                 * @param ctrlBlock
                 */
                override fun onConnectDev(device: UsbDevice?, ctrlBlock: UsbControlBlock?) {
                    Log.d(TAG, "onConnectDev: isExecute--> $isExecute :isCheck-->$isCheck -->isDetach:$isDetach")

                    if (device == null || ctrlBlock == null) {
                        Log.e(TAG, "onConnectDev: Device or CtrlBlock is null")
                        return
                    }

                    if (device != null) {
                        if (uvcCamera == null) {
                            uvcCamera = CameraUVC(context, device)
                            uvcCamera?.setAutoFocus(true)
                        }
                        if (!isDetach) {
                            isDetach = true
                            createUVCInstance()
                            createCapture(UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, UVC_PREVIEW_FPS)
                        }
                    } else {
                        Log.e(TAG, "Permission not granted or device is null")
                    }
                }


                /**
                 * @param device
                 */
                override fun onAttachDev(device: UsbDevice?) {
                    Log.d(TAG, "onAttachDev:isExecute: $isExecute ==> isDetach:$isDetach")
                    if (device == null) {
                        Log.e(TAG, "onAttachDev: Device is null")
                        return
                    }
                    if (!isExecute) {
                        isExecute = true
                        multicam?.register()
                        multicam?.requestPermission(device)
                    }

                    if (!isDetach) {
                        isDetach = true
                        createUVCInstance()
                        createCapture(UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, UVC_PREVIEW_FPS)
                    }
                }
            })
            multicam!!.register()
        } catch (e: Exception) {
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
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
    }

    override fun startCapture(i: Int, i1: Int, i2: Int) {
        Log.d(TAG, "startCapture: ")
        createCapture(i, i1, i2)
    }

    private fun createCapture(i: Int, i1: Int, i2: Int) {
        Log.d(TAG, "createCapture: ")
        if (mUvcStrategy != null) {
            UVC_PREVIEW_WIDTH = i
            UVC_PREVIEW_HEIGHT = i1
            UVC_PREVIEW_FPS = i2
            mUvcStrategy?.setZoom(0)
            mUvcStrategy?.addPreviewDataCallBack(iPreviewDataCallBack)
            mUvcStrategy?.startPreview(cameraRequest, svVideoRender.holder)
        }
        svVideoRender.holder.addCallback(svVideoRender)
    }

    @Throws(InterruptedException::class)
    override fun stopCapture() {
        Log.d(TAG, "stopCapture: ----------------------------------")
        if (mUvcStrategy != null) {
            svVideoRender.release()
            mUvcStrategy?.stopPreview()
            capturerObserver!!.onCapturerStarted(false)
            isExecute = false
        }
    }

    override fun changeCaptureFormat(i: Int, i1: Int, i2: Int) {}
    override fun dispose() {
        Log.d(TAG, "dispose: ******************************************")

        svVideoRender.release()
        surfaceTextureHelper!!.dispose()
        multicam!!.unRegister()

    }

    override fun isScreencast(): Boolean {
        return false
    }

    override fun switchCamera(p0: CameraSwitchHandler?) {

    }

    override fun switchCamera(p0: CameraSwitchHandler?, p1: String?) {

    }

    private val cameraRequest: CameraRequest
        get() = CameraRequest.Builder()
            .setFrontCamera(true)
            .setAspectRatioShow(true)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewWidth(UVC_PREVIEW_WIDTH)
            .setPreviewHeight(UVC_PREVIEW_HEIGHT)
            .create()


    val iPreviewDataCallBack = object : IPreviewDataCallBack {
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {
            if (!isCheck) {

                Log.d(
                    TAG,
                    "onPreviewData() called with: data = $data, width = $width, height = $height, format = $format"
                )
              isCheck=true
            }

            val nv21Buffer = NV21Buffer(data, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, null);
            val frame = VideoFrame(nv21Buffer, 0, System.nanoTime());
            capturerObserver?.onFrameCaptured(frame);
            this@UvcCapturerNew.svVideoRender.onFrame(frame);

        }

    }

    override fun onUVCPreview(iCamera: ICamera, state: ICameraStateCallBack.State, s: String?, callFrom: String) {
        // Log.d(TAG, "onUVCPreview() called with: iCamera = $iCamera, state = $state, s = $s")
        self = iCamera
        code = state
        msg = s
        Log.d(TAG, "onUVCPreview: isExecute=>$isExecute ,isDetach=$isDetach")

        if (!isExecute) {
            isExecute = true
            isDetach = false
            multicam?.register()
            multicam?.requestPermission(iCamera.device)
        }
        if (callFrom.equals("call_activity", true)) {
            isCalledFrom = "call_activity"
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
        if (mUvcStrategy!=null)
        {
            mUvcStrategy?.stopPreview()
            mUvcStrategy?.removePreviewDataCallBack(iPreviewDataCallBack)
            mUvcStrategy?.unRegister()
        }


        multicam?.unRegister()
        isExecute = false
        isDetach = true
//        uvcCamera?.captureVideoStop()
        if (uvcCamera!=null)
        {
            uvcCamera?.captureStreamStop()
            uvcCamera?.closeCamera()
            uvcCamera = null
        }

    }
}



/*
xxxCallActivity         com.codewithkael.firebasevideocall   D  init: isIncoming=Out
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  UvcCapturer:
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  initialize:
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  startCapture:
xxxCallActivity         com.codewithkael.firebasevideocall   D  CallAct -> onObserveEndCall: currentuser:
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onAttachDev: false
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onConnectDev:
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onPreviewData() called with: data = [B@c3
xxxCallActivity         com.codewithkael.firebasevideocall   D  onCameraState: preview-2
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onUVCPreview: true
xxxCallActivity         com.codewithkael.firebasevideocall   D  onCameraState: preview-2
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onUVCPreview: true
xxxCallActivity         com.codewithkael.firebasevideocall   D  onCameraState: preview-2
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onUVCPreview: true

xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onDisConnectDec:
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onDetachDec:

xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onAttachDev: false
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onAttachDev: true
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onAttachDev: true
xxxCallActivity         com.codewithkael.firebasevideocall   D  onCameraState: preview-2
xxxCallUvcCapturer      com.codewithkael.firebasevideocall   D  onUVCPreview: true
*/