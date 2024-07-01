package com.codewithkael.firebasevideocall.videointelegence

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.CountDownTimer
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.codewithkael.firebasevideocall.databinding.ActivityAinavigatorBinding
import com.codewithkael.firebasevideocall.service.MainService
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.ui.LoginActivity
import com.codewithkael.firebasevideocall.utils.setViewFields
import com.codewithkael.firebasevideocall.webrtc.UvcCapturerNew
import com.codewithkael.firebasevideocall.webrtc.WebRTCClient
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraActivity
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.widget.IAspectRatio
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.inject.Inject

private const val TAG = "===>>AINavigator"

@AndroidEntryPoint
class AINavigator : CameraActivity(), UvcCapturerNew.USBPreview, TextToSpeech.OnInitListener {
    @Inject
    lateinit var webRTCClient: WebRTCClient

    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private val viewModel: BakingViewModel by viewModels()
    private lateinit var views: ActivityAinavigatorBinding
    private lateinit var tts: TextToSpeech

    private lateinit var sharedPref: SharedPreferences
    private lateinit var shEdit: SharedPreferences.Editor
    private val handler = android.os.Handler(Looper.getMainLooper())

    private var isPaused = false

    private val prompts = listOf(
        "Detect the surroundings with directions and give the approximate distance of the object in centre of image (left,right,up,down and straight) within 10 words.",
        "Detect the India money and Coins in 8 words also give the .",
        "Identify the commodity in front of the camera only need with accuracy one. Example: 'This is a Colgate toothpaste', 'This is Rin detergent soap'. Limit your description to 7 words."
    )
    private var currentPromptIndex = 0

    override fun getCameraView(): IAspectRatio? {
        Log.d(TAG, "getCameraView: ")

        // Get the width and height
        val width = views.aiRemoteview.width
        val height = views.aiRemoteview.height

        // Calculate the aspect ratio
        val aspectRatio = if (height != 0) width.toFloat() / height.toFloat() else 0f

        // Use the aspect ratio as needed
        println("Aspect Ratio: $aspectRatio")
        //return AspectRatioSurfaceView(this)  //its working internel; cameraactivity surfaceview
        return null
    }

    //It returns the relative layout with id of AIMainRelativeLayout.So that the camera activity can knows where to place and manage camera preview
    override fun getCameraViewContainer(): ViewGroup?//to set the camera preview from the camera activity
    {
        Log.d(TAG, "getCameraViewContainer: ")
        return views.AIMainRelativeLayout
    }

    override fun getRootView(layoutInflater: LayoutInflater): View? {
        Log.d(TAG, "getRootView: ")
        views = ActivityAinavigatorBinding.inflate(layoutInflater)

        sharedPref = getSharedPreferences(setViewFields.PREF_NAME, Context.MODE_PRIVATE)
        shEdit = sharedPref.edit()

        views.apply {
            MainService.localSurfaceView =
                aiRemoteview // set the local surface view (which is in main service) to the airemote view

            UvcCapturerNew.usbPreview =
                this@AINavigator // get this onPreviewStart() data from UvcCapturerNew class

            shEdit.putBoolean(setViewFields.KEY_IS_MIRROR, false)

            // MainService.remoteSurfaceView = remoteView

            LoginActivity.uvc.isUvc.value =
                true // get the UVC status from login activity always true(open)

            //webRTCClient.initLocalSurfaceView(remoteView,isVideoCall = true)

            serviceRepository.AI_setupViews() // getting the local surface view
            webRTCClient.getFrameFromSurface() {
                Log.d(TAG, "getRootView: --------$it")
                viewModel.sendPrompt(it, prompts[currentPromptIndex]) { ans ->
                    handler.postDelayed({
                        views.apply { // The AI result will keep on updating in UI-Text view .So we using runOnUiThread
                            runOnUiThread { ansAi.text = ans }
                        }
                    }, 5000)
                }
            }
        }
        setContentView(views.root)

        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)

        views.ansAi.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                Log.d(TAG, "beforeTextChanged: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d(TAG, "onTextChanged: $s")
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d(TAG, "onTextChanged Editable: $s")
                s?.let {
                    readTextAloud(it.toString())
                }
            }
        })

        views.fabPlay.visibility = View.GONE
        views.fabExit.visibility = View.VISIBLE
        views.fabPause.visibility = View.VISIBLE

        views.apply {
            fabPlay.setOnClickListener {
                Toast.makeText(this@AINavigator, "play", Toast.LENGTH_SHORT).show()
                //tts start
                isPaused = false // Resume playback

                MainService.localSurfaceView =
                    aiRemoteview // set the local surface view (which is in main service) to the airemote view
                UvcCapturerNew.usbPreview =
                    this@AINavigator // get this onPreviewStart() data from UvcCapturerNew class
                MainService.localSurfaceView?.release()
                shEdit.putBoolean(setViewFields.KEY_IS_MIRROR, false)
                LoginActivity.uvc.isUvc.value =
                    true // get the UVC status from login activity always true(open)
                webRTCClient.startCapturingCamera(aiRemoteview)

                //tts start
                readTextAloud(views.ansAi.text.toString())

                fabPlay.visibility = View.GONE
                fabPause.visibility = View.VISIBLE
            }
            fabPause.setOnClickListener {
                Toast.makeText(this@AINavigator, "pause", Toast.LENGTH_SHORT).show()
                webRTCClient.onPreviewStop {}
                isPaused = true // Pause playback
                tts.stop() // Stop current speech

                fabPause.visibility = View.GONE
                fabPlay.visibility = View.VISIBLE
            }
            fabExit.setOnClickListener {
                Toast.makeText(this@AINavigator, "Exit", Toast.LENGTH_SHORT).show()
                webRTCClient.onPreviewStop {}
                finish()
            }
            // Buttons for switching prompts
            btnAINav.setOnClickListener {
                currentPromptIndex = 0
                updatePrompt()
            }
            btnCurrency.setOnClickListener {
                currentPromptIndex = 1
                updatePrompt()
            }
            btnCommodity.setOnClickListener {
                currentPromptIndex = 2
                updatePrompt()
            }
        }
        return views.root
    }

    private fun updatePrompt() {
        viewModel.sendPrompt(null, prompts[currentPromptIndex]) { ans ->
            handler.postDelayed({
                views.apply { // The AI result will keep on updating in UI-Text view. So we using runOnUiThread
                    runOnUiThread { ansAi.text = ans }
                }
            }, 5000)
        }
    }

    //When preview data is received (onPreviewData), it converts the data to a Bitmap, sends it to a view model for processing with a prompt message, and updates the UI with the response asynchronously.
    //onPreviewData is likely called repeatedly as new preview frames are received
    override fun onCameraState(self: MultiCameraClient.ICamera, code: ICameraStateCallBack.State, msg: String?)
    {
        Log.d(TAG, "onCameraState: ")
        /*     val call =
                 object : IPreviewDataCallBack // It handles preview data received from the camera.
                 {
                     override fun onPreviewData(
                         data: ByteArray?,
                         width: Int,
                         height: Int,
                         format: IPreviewDataCallBack.DataFormat
                     ) {
                         Log.d(
                             TAG,
                             "onPreviewData() called with: data = $data, width = $width, height = $height, format = $format"
                         ) // log data check
                         data?.let {
                             views.apply {
                                 if (isRunningCountown) {
                                     isRunningCountown = false
                                     startCountdownTimer(3000, 1000, tvTimer, ansAi, data, width, height)
                                 }
                             }

                             convertYuvToBitmap(data, width, height)?.let { bitmap ->
                                 viewModel.sendPrompt(bitmap, prompts[currentPromptIndex]) { ans ->
                                     handler.postDelayed({
                                         views.apply { // The AI result will keep on updating in UI-Text view. So we using runOnUiThread
                                             runOnUiThread { ansAi.text = ans }
                                         }
                                     }, 5000)
                                 }
                             }
                         }
                     }
                 }
             self.addPreviewDataCallBack(call)*/
    }


    private fun convertYuvToBitmap(yunData: ByteArray, width: Int, height: Int): Bitmap? {
        val yuvImage = YuvImage(yunData, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onStop() {
        super.onStop()
        // uvcPreview?.onCallEnd("")
        // webRTCClient.stopCapturingCamera()
    }

    //onPreviewStart might be called once at the beginning of previewing a specific image or frame.
    override fun onPreviewStart(
        data: ByteArray?,
        width: Int,
        height: Int,
        format: IPreviewDataCallBack.DataFormat
    ) {
      
        data?.let {
            views.apply {
                if (isRunningCountown) {
                    Log.d(TAG, "onPreviewStart: $data")
                    isRunningCountown=false
                    convertYuvToBitmap(data, width, height)?.let { bitmap ->
                        runOnUiThread {
                            startCountdownTimer(5000, 1000, tvTimer, ansAi,bitmap, data, width, height)
                        }
                    }
                }

            }

        }
    }

    var countown: CountDownTimer? = null
    var isRunningCountown: Boolean = true
    fun startCountdownTimer(
        timeInMillis: Long,
        interval: Long,
        textView: TextView,
        ansAi: TextView,
        bitmap: Bitmap,
        data: ByteArray,
        width: Int,
        height: Int
    ) {
        countown = object : CountDownTimer(timeInMillis, interval) {

            override fun onTick(millisUntilFinished: Long) {
                Log.d("TAG_", "onTick: $millisUntilFinished")
                val secondsRemaining = millisUntilFinished / 1000
                textView.text = "$secondsRemaining"
            }

            override fun onFinish() {

                viewModel.sendPrompt(bitmap, prompts[currentPromptIndex]) { ans ->
                    views.apply {
                        runOnUiThread { ansAi.text = ans }
                    }
                }

                Log.d("TAG_", "onFinish() called")
                textView.text = "Time's up!"

            }
        }


            countown?.start()

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            //val result=tts.setLanguage(Locale.US)
            val result = tts.setLanguage(Locale("ta", "IN")) // Setting language to Tamil (India)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Tamil language is not supported", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "TextToSpeech initialized successfully", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }


    private fun readTextAloud(text: String) {
        val maxLength = 4000 // Maximum length TTS can handle in a single call
        val textLength = text.length

        var startIndex = 0

        while (startIndex < textLength) {
            val endIndex =
                if (startIndex + maxLength < textLength) startIndex + maxLength else textLength
            val chunk = text.substring(startIndex, endIndex)
            if (!isPaused) {
                tts.speak(
                    chunk,
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "Chunk-$startIndex"
                ) // Provide a unique utterance ID
                isRunningCountown = true
                Log.d("TAG_l", "readTextAloud: $isRunningCountown")
            }
            startIndex = endIndex
        }
    }


    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        //UvcCapturerNew.usbPreview = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Reinitializing camera and WebRTC client")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Stopping camera capture")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        webRTCClient.onPreviewStop {}
    }
}
