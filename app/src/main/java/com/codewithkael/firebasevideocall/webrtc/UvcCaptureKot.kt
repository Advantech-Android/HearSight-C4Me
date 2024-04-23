package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import androidx.core.content.ContextCompat
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.Size
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener
import com.serenegiant.usb.UVCCamera
import com.serenegiant.usb.UVCParam
import org.webrtc.CameraVideoCapturer
import org.webrtc.CapturerObserver
import org.webrtc.NV21Buffer
import org.webrtc.RendererCommon
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoCapturer
import org.webrtc.VideoFrame
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.abs

class UvcCaptureKot constructor(
    private var context: Context,
    private val svVideoRender: SurfaceViewRenderer
) : VideoCapturer, CameraVideoCapturer, OnDeviceConnectListener, IFrameCallback {
    private val TAG = "===>>UvcCaptureKot"
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var capturerObserver: CapturerObserver? = null
    private var executor: Executor = Executors.newSingleThreadExecutor()
    private var UVC_PREVIEW_WIDTH = 640 //UVCCamera.DEFAULT_PREVIEW_WIDTH

    private var UVC_PREVIEW_HEIGHT = 480 // UVCCamera.DEFAULT_PREVIEW_HEIGHT

    private var UVC_PREVIEW_FPS = 30
    private var camera: UVCCamera? = null
    private var monitor: USBMonitor? = null
    private var statusDOpen = false

    init {
        executor.execute(Runnable {
            monitor = USBMonitor(context, ContextCompat.RECEIVER_EXPORTED, this@UvcCaptureKot)
            monitor?.register()
        })

    }

    override fun initialize(p0: SurfaceTextureHelper?, p1: Context?, p2: CapturerObserver?) {
        surfaceTextureHelper = p0
        context = p1!!
        capturerObserver = p2
    }

    override fun startCapture(p0: Int, p1: Int, p2: Int) {
        Log.d(TAG, "startCapture: ")
        capturerObserver!!.onCapturerStarted(true)
        svVideoRender.addFrameListener({ Log.d(TAG, "onFrame: ====") }, 0f)
    }

    override fun stopCapture() {
        Log.d(TAG, "stopCapture: ")
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.close()
            camera!!.destroy()
            svVideoRender.release()
            capturerObserver!!.onCapturerStarted(false)
        }
    }

    override fun changeCaptureFormat(p0: Int, p1: Int, p2: Int) {
        Log.d(TAG, "changeCaptureFormat() called with: p0 = $p0, p1 = $p1, p2 = $p2")

    }

    override fun dispose() {
        monitor!!.unregister()
        monitor!!.destroy()
        surfaceTextureHelper!!.dispose()
    }

    override fun isScreencast(): Boolean {
        return false
    }

    override fun switchCamera(p0: CameraVideoCapturer.CameraSwitchHandler?) {
        Log.d(TAG, "switchCamera: ")
    }

    override fun switchCamera(p0: CameraVideoCapturer.CameraSwitchHandler?, p1: String?) {
        Log.d(TAG, "switchCamera() called with: p0 = $p0, p1 = $p1")
    }

    override fun onFrame(frame: ByteBuffer?) {
        Log.d(TAG, "onFrame: ------------------------------------------")
        //[Size(2592x1944@15,type:7), Size(2048x1536@15,type:7), Size(1600x1200@15,type:7), Size(1920x1080@30,type:7), Size(1280x960@30,type:7), Size(1280x720@30,type:7), Size(1024x768@30,type:7), Size(960x720@30,type:7), Size(800x600@30,type:7), Size(640x480@30,type:7), Size(320x240@30,type:7), Size(2592x1944@2,type:5), Size(2048x1536@3,type:5), Size(1600x1200@5,type:5), Size(1920x1080@5,type:5), Size(1280x960@5,type:5), Size(1280x720@10,type:5), Size(1024x768@15,type:5), Size(960x720@15,type:5), Size(800x600@20,type:5), Size(640x480@30,type:5), Size(320x240@30,type:5)]
        executor.execute {
            val imageArray = ByteArray(frame!!.remaining())
            frame[imageArray]
            val imageTime = System.nanoTime()
            val nV21Buffer = NV21Buffer(
                imageArray, UVC_PREVIEW_WIDTH, UVC_PREVIEW_HEIGHT
            ) {
                Log.d(
                    TAG,
                    "run: ===================>>>>>>>>>>>>w=640, h=480,>>>>>>>>>>>>>>>>>>>>>>"
                )
            }
            val videoFrame = VideoFrame(nV21Buffer, 0, imageTime)
            capturerObserver!!.onFrameCaptured(videoFrame)
            svVideoRender.onFrame(videoFrame)
        }
    }

    override fun onAttach(device: UsbDevice?) {
        Log.d(TAG, "onAttach: ")
        monitor?.requestPermission(device)
    }

    override fun onDetach(device: UsbDevice?) {
        Log.d(TAG, "onDetach: ")
        statusDOpen = false
    }

    override fun onDeviceOpen(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
        Log.d(TAG, "onDeviceOpen() called with: device = [$device], ctrlBlock = [$ctrlBlock], createNew = [$createNew]")


        if (!statusDOpen) {
            statusDOpen = true
            val list= mutableListOf(UVCCamera.DEFAULT_PREVIEW_FPS,29)
            svVideoRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)



          //  executor.execute {
                camera = UVCCamera(UVCParam(Size(UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT, UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_FPS, list), 1))
                camera!!.open(ctrlBlock)
                try {
                    // onSetPreviewMJEG();
                    // onSetPreviewYUV();
                    val supportedSizes = camera!!.getSupportedSizeList()
                    for (size in supportedSizes) {
                        Log.d(TAG, "Supported Size: $size")
                    }
                    if (supportedSizes.size > 0) {
                        val previewSize = choosePreviewSize(supportedSizes, 16f / 9f, 1200, 700)
                        camera!!.setPreviewSize(previewSize)
                    }else{
                        camera!!.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.UVC_VS_FRAME_MJPEG);
                    }
                    //
                } catch (e: java.lang.IllegalArgumentException) {
                    try {
                        Log.d(TAG, "run: After exception================================================================")
                        camera!!.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_FRAME_FORMAT)
                    } catch (e1: java.lang.IllegalArgumentException) {
                        Log.d(TAG, "run: Error =>" + e1.message)
                        // camera.destroy();
//                        camera = null;
                    }
                }
                if (camera != null) {
                    Log.d(TAG, "run: setPreviewDisplay================================")
                    camera!!.setPreviewDisplay(svVideoRender.holder)
                    camera!!.setFrameCallback(this@UvcCaptureKot, UVCCamera.PIXEL_FORMAT_YUV)
                    camera!!.startPreview()
                } else {
                    Log.d(TAG, "run: nulll")
                }
           // }
        }
    }


    private fun choosePreviewSize(supportedSizes: List<Size>, targetAspectRatio: Float, targetWidth: Int, targetHeight: Int): Size {
        var optimalSize: Size? = null
        var minDiff = Float.MAX_VALUE

        // Calculate the aspect ratio of the target size
        val targetRatio = targetWidth.toFloat() / targetHeight

        // Iterate through the supported sizes
        for (size in supportedSizes) {
            val supportedRatio = size.width.toFloat() / size.height

            // Calculate the difference in aspect ratio
            val aspectRatioDiff = Math.abs(supportedRatio - targetRatio)

            // Check if the current size is closer to the target aspect ratio
            if (aspectRatioDiff < minDiff) {
                optimalSize = size
                minDiff = aspectRatioDiff
            }
        }

        return optimalSize ?: supportedSizes.first()
    }


    private fun onSetPreviewMJEG() {
        val mjpeg_camera_sizes = camera!!.getSupportedSizeList()
        Log.d(TAG, "onDeviceOpen: $mjpeg_camera_sizes")
        // Pick the size that is closest to our required resolution
        val required_width = UVC_PREVIEW_WIDTH //640
        val required_height = UVC_PREVIEW_HEIGHT //480
        val required_area = required_width * required_height
        var preview_width = 0
        var preview_height = 0
        var error = Int.MAX_VALUE // trying to get this as small as possible
        for (s in mjpeg_camera_sizes) {
            // calculate the area for each camera size
            val s_area = s.width * s.height
            // calculate the difference between this size and the target size
            val abs_error = abs((s_area - required_area).toDouble()).toInt()
            // check if the abs_error is smaller than what we have already
            // then use the new size
            if (abs_error < error) {
                preview_width = s.width
                preview_height = s.height
                error = abs_error
            }
        }
        Log.d(
            TAG,
            "run:MJPEG ===> height: $preview_width width: $preview_height"
        )
        try {
            camera!!.setPreviewSize(preview_width, preview_height, UVCCamera.FRAME_FORMAT_MJPEG)
        } catch (ignored: IllegalArgumentException) {
            Log.d(TAG, "onSetPreviewMJEG: Error===>" + ignored.message)
        }
    }

    private fun onSetPreviewYUV() {
        val required_width = 640
        val required_height = 480
        val required_area = required_width * required_height
        // find closest matching size
        // Pick the size that is closest to our required resolution
        var yuv_preview_width = 0
        var yuv_preview_height = 0
        var yuv_error = Int.MAX_VALUE // trying to get this as small as possible
        val yuv_camera_sizes = camera!!.getSupportedSizeList() //UVCCamera.FRAME_FORMAT_YUYV
        for (s in yuv_camera_sizes) {
            // calculate the area for each camera size
            val s_area = s.width * s.height
            // calculate the difference between this size and the target size
            val abs_error = abs((s_area - required_area).toDouble()).toInt()
            // check if the abs_error is smaller than what we have already
            // then use the new size
            if (abs_error < yuv_error) {
                yuv_preview_width = s.width
                yuv_preview_height = s.height
                yuv_error = abs_error
            }
        }
        Log.d(
            TAG,
            "run:YUV===> height: $yuv_preview_height width: $yuv_preview_width"
        )
        camera!!.setPreviewSize(yuv_preview_width, yuv_preview_height, UVCCamera.FRAME_FORMAT_YUYV)
    }

    override fun onDeviceClose(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
        Log.d(TAG, "onDeviceClose: ")
        statusDOpen = false
    }

    override fun onCancel(device: UsbDevice?) {
        Log.d(TAG, "onCancel: ")
    }

    override fun onError(device: UsbDevice?, e: USBMonitor.USBException?) {
        Log.d(TAG, "onError: ${e?.message}")
        super.onError(device, e)
    }
}