package com.codewithkael.firebasevideocall.repository

import android.content.Intent
import android.util.Log
import com.codewithkael.firebasevideocall.firebaseClient.FirebaseClient
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType.*
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.codewithkael.firebasevideocall.webrtc.MyPeerObserver
import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
import com.google.gson.Gson
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "***>>MainRepository"

@Singleton
class MainRepository @Inject constructor(
    private val firebaseClient: FirebaseClient,
    private val webRTCClient: WebRTCClient,
    private val gson: Gson
) : WebRTCClient.Listener {

    private var callerName: String = ""
    private var target: String? = null
    var listener: Listener? = null
    private var remoteView: SurfaceViewRenderer? = null

    fun login(username: String, phonenumber: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.login(username, phonenumber, isDone)
    }

    fun getUserNameFB(phone:String,result: (String?) -> Unit) {
    firebaseClient.getUserNameFB(phone,result)
    }

    fun addContacts(username: String, phone: String, isDone: (Boolean, String?) -> Unit) {
        firebaseClient.addContacts(username, phone, isDone)
    }


    fun observeUsersStatus(
        contactInfoList: (List<ContactInfo>) -> Unit,
        commonContactInfoList: (List<ContactInfo>) -> Unit
    ) {
        firebaseClient.observeContactDetails(contactInfoList, commonContactInfoList)
    }

    fun onObserveEndCall(data: (DataModel, String) -> Unit) {
        firebaseClient.getEndCallEvent(data)
    }

    public fun getUserName(): String {
        return firebaseClient.getUserName()
    }

    public fun getUserPhone(): String {
        return firebaseClient.getUserPhone()
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

    fun setTarget(target: String, callerName: String) {
        this.target = target
        this.callerName = callerName
    }

    interface Listener {
        fun onLatestEventReceived(data: DataModel)
        fun endCall()
    }

    fun initWebrtcClient(username: String) {
        Log.d(TAG, "initWebrtcClient: username: $username")
        webRTCClient.listener = this
        webRTCClient.initializeWebrtcClient(username, object : MyPeerObserver() {

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
            }
        })
    }

    fun initLocalSurfaceView(view: SurfaceViewRenderer, isVideoCall: Boolean) {
        webRTCClient.initLocalSurfaceView(view, isVideoCall)
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCClient.initRemoteSurfaceView(view)
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
        Log.d(TAG, "sendEndCall: target =$target")
        onTransferEventToSocket(
            DataModel(
                type = EndCall,
                target = target!!, targetName = callerName
            )
        )
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

}