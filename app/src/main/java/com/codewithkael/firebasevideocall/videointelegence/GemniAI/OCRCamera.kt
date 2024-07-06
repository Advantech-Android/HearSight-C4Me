package com.codewithkael.firebasevideocall.videointelegence.GemniAI

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

import com.codewithkael.firebasevideocall.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@Suppress("DEPRECATION")
class OCRCamera : AppCompatActivity()
{

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var progressIndicator: ProgressBar
    private lateinit var boundingBoxOverlay: BoundingBoxOverlay
    private lateinit var buttonTakePicture:Button
    private lateinit var textview_extracted_text:TextView
    private lateinit var ttsEngine: TTSEngine
    private lateinit var fab_tts_speak:FloatingActionButton
    private lateinit var fab_tts_stop:FloatingActionButton
    private var isSpeaking=false


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ocrcamera)

        previewView = findViewById(R.id.camera_preview)
        progressIndicator = findViewById(R.id.progress_indicator)
        boundingBoxOverlay = findViewById(R.id.bounding_box_overlay)
        buttonTakePicture=findViewById(R.id.button_take_picture)
        textview_extracted_text=findViewById(R.id.textview_extracted_text)
        fab_tts_speak=findViewById(R.id.fab_tts_speak)
        fab_tts_stop=findViewById(R.id.fab_tts_stop)
        ttsEngine=TTSEngine(this)

        buttonTakePicture.setOnClickListener {
            progressIndicator.visibility = ProgressBar.VISIBLE
            takePicture { text ->
                progressIndicator.visibility = ProgressBar.GONE
                textview_extracted_text.text=text
                //ttsEngine.speakOut(text)
                Toast.makeText(this@OCRCamera, "Text extracted successfully", Toast.LENGTH_SHORT).show()
            }
        }

        fab_tts_speak.setOnClickListener {
            val text = textview_extracted_text.text.toString()
            if (text.isNotEmpty()) {
                if (isSpeaking) {
                    ttsEngine.shutdown()
                }
                ttsEngine.speakOut(text)
                isSpeaking = true
            }
            else
            {
                Toast.makeText(this@OCRCamera, "No text found", Toast.LENGTH_SHORT).show()
            }
        }

        fab_tts_stop.setOnClickListener {
            ttsEngine.shutdown()
            isSpeaking = false
        }


        if (checkPermission(this)) {
            startCamera()
        } else {
            requestPermission(this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetResolution(Size(1280, 720))
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                processImageProxy(imageProxy)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePicture(onCompleteListener: (String) -> Unit) {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                @OptIn(ExperimentalGetImage::class)
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    // Pass the imageProxy to startTextRecognition only if it is not null
                    if (image.image != null) {
                        startTextRecognition(image, onCompleteListener)
                    } else {
                        Log.e("Text-->", "ImageProxy image is null")
                        Toast.makeText(this@OCRCamera, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("Text", "Image capture failed: ${exception.message}")
                    Toast.makeText(this@OCRCamera, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }


    @OptIn(ExperimentalGetImage::class)
    private fun startTextRecognition(imageProxy: ImageProxy, onCompleteListener: (String) -> Unit) {

        if (imageProxy.image == null) {
            Log.e("Text", "ImageProxy or its image is null")
            Toast.makeText(this@OCRCamera, "Failed to process image", Toast.LENGTH_SHORT).show()
            return
        }


        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = processVisionText(visionText)
                onCompleteListener(text)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                imageProxy.close()
            }
    }

    private fun processVisionText(visionText: com.google.mlkit.vision.text.Text): String {
        val text = StringBuilder()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                text.append(line.text).append("\n")
            }
            text.append("\n")
        }
        return text.toString()
    }


    @OptIn(ExperimentalGetImage::class)
    @RequiresApi(Build.VERSION_CODES.P)
    private fun processImageProxy(imageProxy: ImageProxy) {

        val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                drawBoundingBoxes(visionText)
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                imageProxy.close()
            }
    }

    private fun drawBoundingBoxes(visionText: com.google.mlkit.vision.text.Text) {
        val boxes = visionText.textBlocks.flatMap { it.lines }.mapNotNull { it.boundingBox }
        boundingBoxOverlay.setBoundingBoxes(boxes)
    }


    private fun checkPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun requestPermission(context: Context) {
        val launcher = (context as ComponentActivity).registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
                startCamera()
            } else {
                Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        launcher.launch(android.Manifest.permission.CAMERA)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsEngine.shutdown()
    }
}