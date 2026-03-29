package com.goodok.app.webrtc

import android.content.Context
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.webrtc.*
import java.util.*

class WebRTCManager(
    private val context: Context,
    private val callId: String,
    private val isCaller: Boolean
) {
    companion object {
        private const val TAG = "WebRTCManager"

        // Free STUN servers
        private val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer()
        )
    }

    // WebRTC components
    lateinit var rootEglBase: EglBase
    private lateinit var peerConnectionFactory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null

    // Firebase reference
    private val signalingRef = FirebaseDatabase.getInstance().getReference("calls/$callId/signaling")

    // Callbacks
    var onRemoteVideoReceived: ((VideoTrack) -> Unit)? = null
    var onConnected: (() -> Unit)? = null
    var onDisconnected: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val iceCandidates = mutableListOf<IceCandidate>()
    private var isInitiated = false

    fun start(isVideo: Boolean) {
        initializeWebRTC(isVideo)
        createPeerConnection()

        if (isCaller) {
            createOffer()
        } else {
            listenForOffer()
        }

        listenForIceCandidates()
    }

    private fun initializeWebRTC(isVideo: Boolean) {
        // Initialize EGL
        rootEglBase = EglBase.create()

        // Initialize PeerConnectionFactory
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        // Create audio track
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory.createAudioTrack("audio", audioSource)
        localAudioTrack?.setEnabled(true)

        // Create video track if needed
        if (isVideo) {
            createVideoTrack()
        }
    }

    private fun createVideoTrack() {
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            "CaptureThread",
            rootEglBase.eglBaseContext
        )

        videoCapturer = createCameraCapturer()
        if (videoCapturer != null) {
            val videoSource = peerConnectionFactory.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer!!.initialize(surfaceTextureHelper, context, videoSource.capturerObserver)
            videoCapturer!!.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory.createVideoTrack("video", videoSource)
            localVideoTrack?.setEnabled(true)
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        for (deviceName in enumerator.deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    private fun createPeerConnection() {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }

        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> onConnected?.invoke()
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.CLOSED -> onDisconnected?.invoke()
                    PeerConnection.IceConnectionState.FAILED -> onError?.invoke("ICE connection failed")
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "ICE candidate: ${it.sdp}")
                    sendIceCandidate(it)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                streams?.forEach { stream ->
                    stream.videoTracks?.firstOrNull()?.let { videoTrack ->
                        onRemoteVideoReceived?.invoke(videoTrack)
                    }
                }
            }
        })

        // Add tracks
        peerConnection?.addTrack(localAudioTrack, listOf("audio"))
        localVideoTrack?.let { peerConnection?.addTrack(it, listOf("video")) }
    }

    private fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            sendSdp(it)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                onError?.invoke("Create offer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun listenForOffer() {
        signalingRef.child("offer").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sdp = snapshot.getValue(String::class.java)
                sdp?.let {
                    val sessionDescription = SessionDescription(SessionDescription.Type.OFFER, it)
                    peerConnection?.setRemoteDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            createAnswer()
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, sessionDescription)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                sdp?.let {
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            sendSdp(it)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, it)
                }
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                onError?.invoke("Create answer failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun sendSdp(sdp: SessionDescription) {
        val type = if (sdp.type == SessionDescription.Type.OFFER) "offer" else "answer"
        signalingRef.child(type).setValue(sdp.description)

        // Listen for answer if caller
        if (sdp.type == SessionDescription.Type.OFFER) {
            signalingRef.child("answer").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val answerSdp = snapshot.getValue(String::class.java)
                    answerSdp?.let {
                        val sessionDescription = SessionDescription(SessionDescription.Type.ANSWER, it)
                        peerConnection?.setRemoteDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d(TAG, "Remote description set")
                            }
                            override fun onCreateFailure(p0: String?) {}
                            override fun onSetFailure(p0: String?) {}
                        }, sessionDescription)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        val candidateMap = mapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "candidate" to candidate.sdp
        )
        signalingRef.child("candidates/${if (isCaller) "caller" else "callee"}").push().setValue(candidateMap)
    }

    private fun listenForIceCandidates() {
        val path = if (isCaller) "callee" else "caller"
        signalingRef.child("candidates/$path").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val sdpMid = snapshot.child("sdpMid").getValue(String::class.java) ?: return
                val sdpMLineIndex = snapshot.child("sdpMLineIndex").getValue(Int::class.java) ?: return
                val candidate = snapshot.child("candidate").getValue(String::class.java) ?: return

                val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
                peerConnection?.addIceCandidate(iceCandidate)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun setLocalVideoTarget(surfaceView: SurfaceViewRenderer) {
        localVideoTrack?.addSink(surfaceView)
    }

    fun setMute(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun setSpeaker(enabled: Boolean) {
        // Handled by AudioManager
    }

    fun switchCamera() {
        videoCapturer?.let {
            if (it is CameraVideoCapturer) {
                it.switchCamera(null)
            }
        }
    }

    fun endCall() {
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        peerConnection?.close()
        peerConnectionFactory.dispose()
    }
}
