package com.codewithkael.firebasevideocall.ui

import WebQ
import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.QRCode.WifiPasswordGenerated
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


private const val TAG = "LoginActivity"
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var webView1:WebView
    private lateinit var  views:ActivityLoginBinding
    @Inject lateinit var mainRepository: MainRepository
    lateinit var webQ: WebQ
    private val STORAGE_PERMISSION_CODE = 123
    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views.root)
        views.apply {
            Log.d(TAG, "onCreate: ${Build.BRAND}")
            if (Build.BRAND=="samsung"){
                usernameEt.setText("guest")
                passwordEt.setText("111")
            }else
            {
                usernameEt.setText("host")
                passwordEt.setText("222")
            }
        }
        init()
        webQ = WebQ(this)
        views.apply {

            webview.setOnClickListener {
                requestStoragePermission()
                webQ.setupWebView()
                //webView()
            }
        }
    }




    private fun init(){
        views.apply {
            btn.setOnClickListener {
                mainRepository.login(
                    usernameEt.text.toString(),passwordEt.text.toString()
                ){ isDone, reason ->
                    if (!isDone){
                        Toast.makeText(this@LoginActivity, reason, Toast.LENGTH_SHORT).show()
                    }else{
                        //start moving to our main activity
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("username",usernameEt.text.toString())
                        })
                    }
                }
            }
        }
    }


    //Requesting permission
    fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openFileExplorer()
            return
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
            //And finally ask for the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }

    }
    private fun openFileExplorer() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.setType("image/*")
        this@LoginActivity.startActivityForResult(
            Intent.createChooser(i, "File Chooser"),
           FILECHOOSER_RESULTCODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {

           super.onRequestPermissionsResult(requestCode, permissions, grantResults)


    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {

        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu,menu)
        menu!!.findItem(R.id.setting).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.qrCode->{
                val wifipassgenerator= WifiPasswordGenerated(this)
                wifipassgenerator.showWifi()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}