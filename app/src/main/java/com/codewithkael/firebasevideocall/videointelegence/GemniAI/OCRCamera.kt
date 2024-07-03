package com.codewithkael.firebasevideocall.videointelegence.GemniAI

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.codewithkael.firebasevideocall.databinding.ActivityOcrcameraBinding
import com.google.mlkit.vision.common.InputImage

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions



class OCRCamera : AppCompatActivity() {
    private lateinit var OCRbinding: ActivityOcrcameraBinding

    private companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    private var imageUri: Uri? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private lateinit var textRecognizer: TextRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OCRbinding = ActivityOcrcameraBinding.inflate(layoutInflater)
        setContentView(OCRbinding.root)
        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        textRecognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        OCRbinding.btnTakeImage.setOnClickListener {
            showInputImageDialog()
        }
        OCRbinding.btnTextExtract.setOnClickListener {
            if(imageUri==null){
                Toast.makeText(this, "Pick image first", Toast.LENGTH_SHORT).show()
            }
            else
            {
                recognizeTextFromImage()
            }
        }

    }

    private fun recognizeTextFromImage()
    {
       try {
           val inputImage=InputImage.fromFilePath(this,imageUri!!)
           val textResult=textRecognizer.process(inputImage).addOnSuccessListener {text->
               val recognizedText=text.text
               OCRbinding.txtDisplay.text = recognizedText
           }
               .addOnFailureListener {e->
                   Toast.makeText(this, "Failed to recognize ${e.message.toString()}", Toast.LENGTH_SHORT).show()
               }
       }
       catch (e:Exception)
       {
           Toast.makeText(this, "Failed to recognize ${e.message.toString()}", Toast.LENGTH_SHORT).show()
       }
    }

    private fun showInputImageDialog() {
        val popupMenu=PopupMenu(this,OCRbinding.btnTakeImage)
        popupMenu.menu.add(Menu.NONE,1,1,"CAMERA")
        popupMenu.menu.add(Menu.NONE,2,2,"GALLERY")
        popupMenu.show()

        popupMenu.setOnMenuItemClickListener {menuItem->
            val id=menuItem.itemId
            if(id==1)
            {
                if(checkCameraPermissions()){
                    pickImageCamera()
                }
                else
                {
                    requestCameraPermissions()
                }
            }
            else if(id==2)
            {
                if(checkStoragePermission()){
                    pickImageGallery()
                }
                else
                {
                    requestStoragePermission()
                }
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val data = res.data
                imageUri = data!!.data
                OCRbinding.imageIv.setImageURI(imageUri)
            } else {
                Toast.makeText(this@OCRCamera, "Cancelled..", Toast.LENGTH_SHORT).show()
            }
        }

    private fun pickImageCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "SampleTitle")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                OCRbinding.imageIv.setImageURI(imageUri)
            } else {
                Toast.makeText(this@OCRCamera, "Cancelled", Toast.LENGTH_SHORT).show()
            }

        }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPermissions(): Boolean {
        val cameraResult = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val storageResult = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        return cameraResult && storageResult
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermissions() {
        ActivityCompat.requestPermissions(this, storagePermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (cameraAccepted && storageAccepted) {
                        pickImageCamera()
                    } else {
                        Toast.makeText(this@OCRCamera, "Camer and Storage Permission required", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {
                if(grantResults.isNotEmpty()){
                    val storageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED
                    if(storageAccepted){
                        pickImageGallery()
                    }
                    else
                    {
                        Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        }
    }
}






