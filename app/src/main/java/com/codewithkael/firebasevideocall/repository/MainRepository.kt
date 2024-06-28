package com.codewithkael.firebasevideocall.repository

import android.content.Intent
import android.util.Log
import com.codewithkael.firebasevideocall.firebaseClient.FirebaseClient
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.ui.MainActivity
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType.*
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.codewithkael.firebasevideocall.webrtc.MyPeerObserver
import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

//
private const val TAG = "***>>MainRepository"

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
) : WebRTCClient.Listener {

    private var target: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null

    fun login(
        username: String,
        phonenumber: String,
        status: String,
        isLogin: Boolean,
        isDone: (Boolean, String?) -> Unit
    ) {
        firebaseClient.login(username, phonenumber, status, isLogin, isDone)
    }

    fun getUserNameFB(phone: String, result: (String?) -> Unit) {
        firebaseClient.getUserNameFB(phone, result)
    }

    fun addContacts(username: String, phone: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.addContacts(username, phone, isDone)
    }


    suspend fun observeUsersStatus(
        ctx: MainActivity,
        contactInfoList: (List<ContactInfo>) -> Unit,
        commonContactInfoList: (List<ContactInfo>) -> Unit,
        noAccountContactInfoList: (List<ContactInfo>) -> Unit
    ) {
        firebaseClient.observeContactDetails(
            ctx,
            contactInfoList,
            commonContactInfoList,
            noAccountContactInfoList
        )
    }

    fun onObserveEndCall(data: (DataModel, String) -> Unit) {
        firebaseClient.getEndCallEvent(data)
    }


    public fun getUserPhone(): String {
        return firebaseClient.getUserPhone()
    }

    public fun setUsernameAndPhone(username: String, phonenumber: String) {
        firebaseClient.setUsername(username, phonenumber)
    }

    fun initFirebase() {
        firebaseClient.subscribeForLatestEvent(object : FirebaseClient.Listener {
            override fun onLatestEventReceived(event: DataModel) {
                listener?.onLatestEventReceived(event)
                when (event.type) {
                    Offer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.OFFER,
                                event.data.toString()
                            )
                        )
                        webRTCClient.answer(target!!)
                    }

                    Answer -> {
                        webRTCClient.onRemoteSessionReceived(
                            SessionDescription(
                                SessionDescription.Type.ANSWER,
                                event.data.toString()
                            )
                        )
                    }

                    IceCandidates -> {
                        val candidate: IceCandidate? = try {
                            gson.fromJson(event.data.toString(), IceCandidate::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        candidate?.let {
                            webRTCClient.addIceCandidateToPeer(it)
                        }
                    }

                    EndCall -> {
                        listener?.endCall()
                    }

                    else -> Unit
                }
            }

        })
    }

    fun removeContactListener() =
        firebaseClient.removeContactListener()

    fun sendConnectionRequest(target: String, isVideoCall: Boolean, success: (Boolean) -> Unit) {
        firebaseClient.sendMessageToOtherClient(
            DataModel(
                type = if (isVideoCall) StartVideoCall else StartAudioCall,
                target = target
            ), success
        )
        Log.d(
            TAG,
            "sendConnectionRequest: TARGET \t${target.toString()} isVideoCall::\t${isVideoCall}"
        )
    }

    fun setTarget(target: String) {
        this.target = target
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
    }

    fun closeConnection() {
        webRTCClient.closeConnection()

    }

    fun initWebrtcClient(username: String) {
        Log.d(TAG, "initWebrtcClient: username: $username")
        webRTCClient.listener = this
        val peerObserver = object : MyPeerObserver() {
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {
                super.onSignalingChange(p0)
                Log.d(TAG, "onSignalingChange: ")
            }

            override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {
                super.onIceConnectionChange(p0)
                Log.d(TAG, "onIceConnectionChange: ")
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {
                super.onIceConnectionReceivingChange(p0)
                Log.d(TAG, "onIceConnectionReceivingChange: ")
            }

            override fun onDataChannel(p0: DataChannel?) {
                super.onDataChannel(p0)
                Log.d(TAG, "onDataChannel: ")
            }

            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {
                super.onIceGatheringChange(p0)
                Log.d(TAG, "onIceGatheringChange: ")
            }

            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {
                super.onIceCandidatesRemoved(p0)
                Log.d(TAG, "onIceCandidatesRemoved: ")
            }

            override fun onRemoveStream(p0: MediaStream?) {
                super.onRemoveStream(p0)
                Log.d(TAG, "onRemoveStream: ")
            }

            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                super.onStandardizedIceConnectionChange(newState)
                Log.d(TAG, "onStandardizedIceConnectionChange: ")
            }

            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {
                super.onAddTrack(p0, p1)
                Log.d(TAG, "onAddTrack: ")
            }

            override fun onTrack(transceiver: RtpTransceiver?) {
                super.onTrack(transceiver)
                Log.d(TAG, "onTrack: ")
            }

            override fun onRenegotiationNeeded() {
                super.onRenegotiationNeeded()
                Log.d(TAG, "onRenegotiationNeeded: ")
            }

            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent?) {
                super.onSelectedCandidatePairChanged(event)
                Log.d(TAG, "onSelectedCandidatePairChanged: ")
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                try {
                    p0?.videoTracks?.get(0)?.addSink(remoteView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let {
                    webRTCClient.sendIceCandidate(target!!, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                if (newState == PeerConnection.PeerConnectionState.CONNECTED) {
                    // 1. change my status to in call
                    changeMyStatus(UserStatus.IN_CALL)
                    // 2. clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                }
                if (newState == PeerConnection.PeerConnectionState.DISCONNECTED) {
                    // 1. change my status to online
                    changeMyStatus(UserStatus.ONLINE)
                    // 2. clear latest event inside my user section in firebase database
                    firebaseClient.clearLatestEvent()
                    //   webRTCClient. createReConnectPeerConnection(peerObserver)

                }
            }
        }
        webRTCClient.initializeWebrtcClient(username, peerObserver)

    }

    fun initLocalSurfaceView(
        view: SurfaceViewRenderer,
        isVideoCall: Boolean,
        isAINavigator: Boolean
    ) {
        webRTCClient.initLocalSurfaceView(view, isVideoCall, isAINavigator)

    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer, isAINavigator: Boolean) {
        webRTCClient.initRemoteSurfaceView(view, isAINavigator)
        this.remoteView = view
    }

    fun startCall() {
        webRTCClient.call(target!!)
    }

    fun endCall() {
        webRTCClient.closeConnection()
        changeMyStatus(UserStatus.ONLINE)
    }

    fun sendEndCall() {

        if (target?.isNullOrEmpty() == false) {
            Log.d(TAG, "sendEndCall: target =$target")
            onTransferEventToSocket(
                DataModel(type = EndCall, target = target!!)
            )

        } else {
            onTransferEventToSocket(
                DataModel(type = EndCall, target = "")
            )
        }
    }

    private fun changeMyStatus(status: UserStatus) {
        firebaseClient.changeMyStatus(status)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        webRTCClient.toggleAudio(shouldBeMuted)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        webRTCClient.toggleVideo(shouldBeMuted)
    }

    fun switchCamera() {
        webRTCClient.switchCamera()
    }

    override fun onTransferEventToSocket(data: DataModel) {
        firebaseClient.sendMessageToOtherClient(data) {}
    }

    fun setScreenCaptureIntent(screenPermissionIntent: Intent) {
        webRTCClient.setPermissionIntent(screenPermissionIntent)
    }

    fun toggleScreenShare(isStarting: Boolean) {
        if (isStarting) {
            webRTCClient.startScreenCapturing()
        } else {
            webRTCClient.stopScreenCapturing()
        }
    }

    fun logOff(function: () -> Unit) = firebaseClient.logOff(function)

    fun setCallStatus(
        target: String,
        sender: String,
        callLogs: String,
        callStatus: (String) -> Unit
    ) {
        //here sender act as caller change
        //here target act as receiver change
        firebaseClient.sendCallStatusToOtherClient(target, sender, callLogs, callStatus)
    }

    fun onPreviewStop(callBack: () -> Unit) {
        webRTCClient.onPreviewStop(callBack)
    }
}