package com.codewithkael.firebasevideocall.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.projection.MediaProjection
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.core.graphics.scaleMatrix
import com.codewithkael.firebasevideocall.ui.LoginActivity.uvc.isUvc
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "###CallWebRTCClient"

@Singleton
class WebRTCClient @Inject constructor(private val context: Context, private val gson: Gson) {
    //  private var isUvc = false
    //class variables
    lateinit var uvcCapturer: CameraVideoCapturer


    var listener: Listener? = null
    private lateinit var username: String
    private lateinit var usbCapturer: CameraVideoCapturer

    //webrtc variables
    private val eglBaseContext = EglBase.create().eglBaseContext
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private var peerConnection: PeerConnection? = null
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("turn:a.relay.metered.ca:443?transport=tcp")
            .setUsername("83eebabf8b4cce9d5dbcb649").setPassword("2D7JvfkOQtBdYW3R")
            .createIceServer(),
        PeerConnection.IceServer(
            "turn:openrelay.metered.ca:80",
            "openrelayproject",
            "openrelayproject"
        ),
        PeerConnection.IceServer(
            "turn:openrelay.metered.ca:443",
            "openrelayproject",
            "openrelayproject"
        ),
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478")
            .setUsername("83eebabf8b4cce9d5dbcb612").setPassword("2D7JvfkOQtBdYW5t")
            .createIceServer(),
        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),
    )

    private val localVideoSource by lazy {


        if (isUvc.value == true)
            peerConnectionFactory.createVideoSource(usbCapturer.isScreencast)
        else
            peerConnectionFactory.createVideoSource(false)
    }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val videoCapturer = getVideoCapturer(context)
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private val mediaConstraint = MediaConstraints()
        .apply {
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
                DefaultVideoEncoderFactory(eglBaseContext, true, true)
            ).setOptions(PeerConnectionFactory.Options().apply {
                disableNetworkMonitor = false
                disableEncryption = false
            }).createPeerConnectionFactory()
    }

    fun initializeWebrtcClient(username: String, observer: PeerConnection.Observer) {
        Log.d(TAG, "initializeWebrtcClient: ")
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    fun createReConnectPeerConnection(username: String, observer: PeerConnection.Observer) {
        this.username = username
        localTrackId = "${username}_track"
        localStreamId = "${username}_stream"
        peerConnection = createPeerConnection(observer)
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        Log.d(TAG, "createPeerConnection: ")
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
        Log.d(TAG, "answer: sender: " + target + ", target: " + target)
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
        Log.d(TAG, "onRemoteSessionReceived: ")
        peerConnection?.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        Log.d(TAG, "addIceCandidateToPeer: ")
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(target: String, iceCandidate: IceCandidate) {
        Log.d(TAG, "sendIceCandidate: ")
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
        Log.d(TAG, "closeConnection: ")
        try {
            videoCapturer?.dispose()
            screenCapturer?.dispose()
            localStream?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            // e.printStackTrace()
            Log.e(TAG, "closeConnection: ${e.message}")
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
    private fun initSurfaceView(
        view: SurfaceViewRenderer,
        isLocal: Boolean,
        isAINavigator: Boolean
    ) {
        Log.d(
            TAG,
            "initSurfaceView() called with: view = $view, isLocal = $isLocal, isAINavigator = $isAINavigator"
        )

        view.run {
//          release()
//            if (isLocal)
//                setMirror(true)
//            else
//                setMirror(false)

            if (isAINavigator) {

                setMirror(false)

            } else {
                setMirror(false)
            }

            if (isUvc.value == false) {
                scaleX = 1.0f
                scaleY = 1.0f
            }
            setEnableHardwareScaler(true)
            setZOrderMediaOverlay(true)
            scaleMatrix()
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)



            init(eglBaseContext, object : RendererCommon.RendererEvents {
                override fun onFirstFrameRendered() {
                    Log.d(TAG, "onFirstFrameRendered: ")
                }

                override fun onFrameResolutionChanged(p0: Int, p1: Int, p2: Int) {
                    // Log.d(TAG, "$p0,$p1,$p2")
                    Log.d(TAG, "initSurfaceView: ${cameraDistance}")
                }
            })


        }
    }


    fun initRemoteSurfaceView(remoteView: SurfaceViewRenderer, isAINavigator: Boolean) {
        this.remoteSurfaceView = remoteView
        initSurfaceView(remoteView, isLocal = false, isAINavigator)
    }

    fun initLocalSurfaceView(
        localView: SurfaceViewRenderer,
        isVideoCall: Boolean,
        isAINavigator: Boolean
    ) {
        this.localSurfaceView = localView
        initSurfaceView(localView, isLocal = true, isAINavigator)
        startLocalStreaming(localView, isVideoCall)
    }

    private fun startLocalStreaming(localView: SurfaceViewRenderer, isVideoCall: Boolean) {
        // setAudioOutputToSpeaker(context)
        Log.d(TAG, "startLocalStreaming: ")
        localSurfaceView = localView
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        if (isVideoCall) {
            startCapturingCamera(localView)
        }
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(localTrackId + "_audio", localAudioSource)
        localAudioTrack?.setEnabled(true)
        localAudioTrack?.setVolume(1.0)
        localStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }



    fun setAudioOutputToSpeaker(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true
    }

    fun startCapturingCamera(localView: SurfaceViewRenderer) {
        Log.d(TAG, "startCapturingCamera: ")
        localSurfaceView = localView
        surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name, eglBaseContext
        )
        if (isUvc.value == true) {
            Toast.makeText(
                context,
                "uvc support=${Camera2Enumerator.isSupported(context)}",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "startCapturingCamera: ${Camera2Enumerator(context).deviceNames}")
            uvcCapturer = UvcCapturerNew(context, localView, eglBaseContext)
            var localVideoSource = peerConnectionFactory.createVideoSource(uvcCapturer.isScreencast)
            uvcCapturer.initialize(
                surfaceTextureHelper,
                context,
                localVideoSource.capturerObserver
            )
            uvcCapturer.startCapture(1280, 720, 30)
            localVideoTrack =
                peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        } else {
            videoCapturer?.initialize(
                surfaceTextureHelper, context, localVideoSource.capturerObserver
            )
            videoCapturer?.startCapture(720, 480, 30)
            localVideoTrack =
                peerConnectionFactory.createVideoTrack(localTrackId + "_video", localVideoSource)
        }
        Log.d(TAG, "startCapturingCamera: ")
        // localVideoTrack?.addSink(localView)
        localVideoTrack?.addSink(localSurfaceView)
        localStream?.addTrack(localVideoTrack)
    }


    private fun getVideoCapturer(context: Context): CameraVideoCapturer? =

        if (isUvc.value == true) {

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

    fun stopCapturingCamera() {
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
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, eglBaseContext)
        screenCapturer = createScreenCapturer()
        screenCapturer!!.initialize(
            surfaceTextureHelper,
            context,
            localScreenVideoSource.capturerObserver
        )
        screenCapturer!!.startCapture(screenWidthPixels, screenHeightPixels, 15)
        localScreenShareVideoTrack =
            peerConnectionFactory.createVideoTrack(localTrackId + "_video", localScreenVideoSource)
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

    @SuppressLint("SuspiciousIndentation")
    fun onPreviewStop(callBack: () -> Unit) {
        val uvcCapturerNew = uvcCapturer as UvcCapturerNew
        uvcCapturerNew.onPreviewStop(callBack)
    }

    interface Listener {
        fun onTransferEventToSocket(data: DataModel)
    }
}


