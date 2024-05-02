package com.codewithkael.firebasevideocall.webrtc

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.codewithkael.firebasevideocall.ui.CallActivity
import com.codewithkael.firebasevideocall.ui.LoginActivity.uvc.isUvc
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.google.gson.Gson
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "xxx--->>>WebRTCClient"
@Singleton
class WebRTCClient @Inject constructor(
    private val context: Context,
    private val gson: Gson
) {
    //class variables
    var listener: Listener? = null
    private lateinit var username: String
   // private var isUvc = true
    private lateinit var usbCapturer: CameraVideoCapturer
    private var mCameraHelper: ICameraHelper? = null
    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp").setUsername("83eebabf8b4cce9d5dbcb649").setPassword("2D7JvfkOQtBdYW3R").createIceServer(),

        PeerConnection.IceServer("turn:openrelay.metered.ca:80",
            "openrelayproject",
            "openrelayproject"),
        PeerConnection.IceServer(
            "turn:openrelay.metered.ca:443",
            "openrelayproject",
            "openrelayproject"
        ),
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478")
            .setUsername("83eebabf8b4cce9d5dbcb612")
            .setPassword("2D7JvfkOQtBdYW5t").createIceServer(),
        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),
    )

    private val localVideoSource by lazy {
        if (isUvc.value==true)
            peerConnectionFactory.createVideoSource(usbCapturer.isScreencast)
        else
            peerConnectionFactory.createVideoSource(false)
    }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    //call variables
    private lateinit var localSurfaceView: SurfaceViewRenderer
    private lateinit var remoteSurfaceView: SurfaceViewRenderer
    private var localStream: MediaStream? = null
    private var localTrackId = ""
    private var localStreamId = ""
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null

    //screen casting
    private var permissionIntent: Intent? = null
    private var screenCapturer: VideoCapturer? = null
    private val localScreenVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private var localScreenShareVideoTrack: VideoTrack? = null

    //installing requirements section
    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(
                DefaultVideoDecoderFactory(eglBaseContext)
            ).setVideoEncoderFactory(
                DefaultVideoEncoderFactory(
                    eglBaseContext, true, true
                )
            ).setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }

    fun onCameraHelper(onCameraHelper: ICameraHelper)
    {
        mCameraHelper=onCameraHelper as ICameraHelper
    }

    fun initializeWebrtcClient(
        username: String, observer: PeerConnection.Observer
    ) {
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServer, observer)
    }

    //negotiation section
    fun call(target: String) {
        Log.d(TAG, "call: sender: $username target: $target")
        peerConnection?.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(
                                type = DataModelType.Offer,
                                sender = username,
                                target = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)

    }

    fun answer(target: String) {
        Log.d(TAG, "answer: sender: " + target+", target: " + target)
        peerConnection?.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection?.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            DataModel(
                                type = DataModelType.Answer,
                                sender = username,
                                target = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection?.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
        addIceCandidateToPeer(iceCandidate)
        listener?.onTransferEventToSocket(
            DataModel(
                type = DataModelType.IceCandidates,
                sender = username,
                target = target,
                data = gson.toJson(iceCandidate)
            )
        )
    }

    fun closeConnection() {
        try {
            videoCapturer?.dispose()
            screenCapturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        if (shouldBeMuted) {
            localStream?.removeTrack(localAudioTrack)
        } else {
            localStream?.addTrack(localAudioTrack)
        }
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        try {
            if (shouldBeMuted) {
                stopCapturingCamera()
            } else {
                startCapturingCamera(localSurfaceView)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //streaming section
    private fun initSurfaceView(view: SurfaceViewRenderer) {
        view.run {
            //release()
            setMirror(false)
            setEnableHardwareScaler(true)
            init(eglBaseContext, null)
        }
    }

    fun initRemoteSurfaceView(remoteView: SurfaceViewRenderer) {
        this.remoteSurfaceView = remoteView
        initSurfaceView(remoteView)
    }

    fun initLocalSurfaceView(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        this.localSurfaceView = localView
        initSurfaceView(localView)
        startLocalStreaming(localView, isVideoCall)
    }

    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        localSurfaceView=localView
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if (isVideoCall) {
            startCapturingCamera(localView)
        }
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    private fun startCapturingCamera(localView: SurfaceViewRenderer) {
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )

        if (isUvc.value==true) {
            usbCapturer = UvcCapturer(context, localView) as CameraVideoCapturer
         usbCapturer.initialize(surfaceTextureHelper,context,localVideoSource.capturerObserver)

            usbCapturer.startCapture(
                720, 480, 20
            )
        } else {
            videoCapturer?.initialize(
                surfaceTextureHelper, context, localVideoSource.capturerObserver
            )
            videoCapturer?.startCapture(
                720, 480, 20
            )
        }


        val localVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        localVideoTrack?.addSink(localView)
        localStream?.addTrack(localVideoTrack)
    }


    private fun getVideoCapturer(context: Context): CameraVideoCapturer? =

        if (isUvc.value==true) {

            // create USBCapturer (USB Camera)
         null
        } else {
            Camera2Enumerator(context).run {
                deviceNames.find {
                    isFrontFacing(it)
                }?.let {
                    createCapturer(it, null)
                } ?: throw IllegalStateException()
            }
        }

    private fun stopCapturingCamera() {
        Log.d(TAG, "stopCapturingCamera: ")
        videoCapturer?.dispose()
        localVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localVideoTrack)
        localVideoTrack?.dispose()
    }

    //screen capture section

    fun setPermissionIntent(screenPermissionIntent: Intent) {
        this.permissionIntent = screenPermissionIntent
    }

    fun startScreenCapturing() {
        val displayMetrics = DisplayMetrics()
        val windowsManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowsManager.defaultDisplay.getMetrics(displayMetrics)

        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels

        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)


        screenCapturer = createScreenCapturer()
        screenCapturer!!.initialize(
            surfaceTextureHelper, context, localScreenVideoSource.capturerObserver
        )
        screenCapturer!!.startCapture(screenWidthPixels, screenHeightPixels, 15)

        localScreenShareVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId + "_video", localScreenVideoSource)

        localScreenShareVideoTrack?.addSink(localSurfaceView)
        localStream?.addTrack(localScreenShareVideoTrack)
        peerConnection?.addStream(localStream)

    }

    fun stopScreenCapturing() {
        screenCapturer?.stopCapture()
        screenCapturer?.dispose()
        localScreenShareVideoTrack?.removeSink(localSurfaceView)
        localSurfaceView.clearImage()
        localStream?.removeTrack(localScreenShareVideoTrack)
        localScreenShareVideoTrack?.dispose()

    }

    private fun createScreenCapturer(): VideoCapturer {
        return ScreenCapturerAndroid(permissionIntent, object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("permissions", "onStop: permission of screen casting is stopped")
            }
        })
    }


    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }
}