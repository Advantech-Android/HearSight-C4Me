package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import android.widget.Toast
import com.codewithkael.firebasevideocall.ui.CallActivity
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IDeviceConnectCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.usb.USBMonitor
import com.journeyapps.barcodescanner.camera.CameraInstance
import org.webrtc.CameraVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.NV21Buffer
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UvcCapturerOld(context: Context, svVideoRender: SurfaceViewRenderer) :
    CameraInstance(context), VideoCapturer, CameraVideoCapturer,
    CallActivity.UVCPreview {
    private val context: Context
    private var svVideoRender: SurfaceViewRenderer
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturerObserver: CapturerObserver? = null
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private var UVC_PREVIEW_WIDTH = 1280
    private var UVC_PREVIEW_HEIGHT = 720
    private var UVC_PREVIEW_FPS = 30
    var mUvcStrategy: CameraUVC? = null // From Jiang Dongguo's AUSBC library
    var self: MultiCameraClient.ICamera? = null
    var code: ICameraStateCallBack.State? = null
    var msg: String? = null
    var multicam: MultiCameraClient? = null
    var isExecute = false

    init {
        Log.d(TAG, "UvcCapturer: ")
        this.context = context
        this.svVideoRender = svVideoRender
        CallActivity.uvcPreview = this

        try {
            multicam = MultiCameraClient(context, object : IDeviceConnectCallBack {
                /**
                 * @param device
                 */
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
                /**
                 * @param device
                 * @param ctrlBlock
                 */
                override fun onDisConnectDec(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                    Log.d(TAG, "onDisConnectDec: ")
                }

                /**
                 * @param device
                 * @param ctrlBlock
                 */
                /**
                 * @param device
                 * @param ctrlBlock
                 */
                override fun onConnectDev(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                    Log.d(TAG, "onConnectDev: ")

                    mUvcStrategy!!.addPreviewDataCallBack(iPreviewDataCallBack);
                }

                /**
                 * @param device
                 */
                /**
                 * @param device
                 */
                override fun onDetachDec(device: UsbDevice?) {
                    Log.d(TAG, "onDetachDec: ")
                    multicam?.unRegister()
                    isExecute=false
                }

                /**
                 * @param device
                 */
                /**
                 * @param device
                 */
                override fun onAttachDev(device: UsbDevice?) {
                    Log.d(TAG, "onAttachDev: ")
                    if (!isExecute) {
                        if (device != null) {
                            mUvcStrategy = CameraUVC(context, device)
                        }

                        multicam!!.requestPermission(device)
                        isExecute = true
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
        if (mUvcStrategy != null) {
            UVC_PREVIEW_WIDTH = i
            UVC_PREVIEW_HEIGHT = i1
            UVC_PREVIEW_FPS = i2
            mUvcStrategy!!.setZoom(0)


            // self.addPreviewDataCallBack(iPreviewDataCallBack);
            mUvcStrategy?.onReady(svVideoRender.holder.surface)
        }
        svVideoRender.holder.addCallback(svVideoRender)
    }

    @Throws(InterruptedException::class)
    override fun stopCapture() {
        if (mUvcStrategy != null) {
            multicam!!.unRegister()
            svVideoRender.release()
            capturerObserver!!.onCapturerStarted(false)
            isExecute=false
        }
    }

    override fun changeCaptureFormat(i: Int, i1: Int, i2: Int) {}
    override fun dispose() {
        surfaceTextureHelper!!.dispose()
    }

    override fun isScreencast(): Boolean {
        return false
    }

    override fun switchCamera(p0: CameraVideoCapturer.CameraSwitchHandler?) {

    }

    override fun switchCamera(p0: CameraVideoCapturer.CameraSwitchHandler?, p1: String?) {

    }

    private val cameraRequest: CameraRequest
        private get() = CameraRequest.Builder()
            .setFrontCamera(true)
            .setAspectRatioShow(true)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .setPreviewWidth(UVC_PREVIEW_WIDTH)
            .setPreviewHeight(UVC_PREVIEW_HEIGHT)
            .create()


    companion object {
        private const val TAG = "xxxCallUvcCapturer"

    }

    val iPreviewDataCallBack = object : IPreviewDataCallBack {
        override fun onPreviewData(
            data: ByteArray?,
            width: Int,
            height: Int,
            format: IPreviewDataCallBack.DataFormat
        ) {
            Log.d(
                TAG,
                "onPreviewData() called with: data = $data, width = $width, height = $height, format = $format"
            )

            val nv21Buffer = NV21Buffer(data, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT, null);
            val frame = VideoFrame(nv21Buffer, 0, System.nanoTime());
            capturerObserver?.onFrameCaptured(frame);
            this@UvcCapturerOld.svVideoRender.onFrame(frame);
            mUvcStrategy?.onReady(this@UvcCapturerOld.svVideoRender.holder.surface)
        }

    }

    override fun onUVCPreview(iCamera: MultiCameraClient.ICamera,
                              state: ICameraStateCallBack.State, s: String?,callFrom:String) {
        // Log.d(TAG, "onUVCPreview() called with: iCamera = $iCamera, state = $state, s = $s")
        self = iCamera
        code = state
        msg = s

        executor.execute {
            iCamera.addPreviewDataCallBack(iPreviewDataCallBack)
        }
    }

    override fun onCallEnd(msg: String) {

    }
}