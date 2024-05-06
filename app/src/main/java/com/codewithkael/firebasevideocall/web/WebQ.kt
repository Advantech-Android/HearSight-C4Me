import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.utils.SnackBarUtils
import com.codewithkael.firebasevideocall.utils.WebQFields

class WebQ(private val context: Context) {

    private lateinit var webView: WebView
    private val STORAGE_PERMISSION_CODE = 123
    private val FILECHOOSER_RESULTCODE = 1
    var mUploadMessage: ValueCallback<Array<Uri>>? = null


    fun setupWebView() {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.addcontacts)
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        dialog.show()

        webView = dialog.findViewById(R.id.webView1)
        webView.loadUrl("https://4ba5-103-102-96-250.ngrok-free.app/")
        initWebView()

        webView.setOnTouchListener { _, event ->
            handleWebViewTouchEvent(event)
        }
    }

    private fun handleWebViewTouchEvent(event: MotionEvent?): Boolean {
        when (event?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("TouchEvent", "Action DOWN at (${event.x}, ${event.y})")
            }
            MotionEvent.ACTION_MOVE -> {
                Log.d("TouchEvent", "Action MOVE at (${event.x}, ${event.y})")
            }
            MotionEvent.ACTION_UP -> {
                requestStoragePermission()
                Log.d("TouchEvent", "Action UP at (${event.x}, ${event.y})")
            }
        }
        return false
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openFileExplorer()
        } else {
            ActivityCompat.requestPermissions(
                context as AppCompatActivity,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        (context as AppCompatActivity).startActivityForResult(
            Intent.createChooser(intent, "File Chooser"),
            FILECHOOSER_RESULTCODE
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileExplorer()
                //Toast.makeText(context, "Permission granted to read storage", Toast.LENGTH_SHORT).show()
                SnackBarUtils.showSnackBar(webView,WebQFields.PERMISSION_GRANTED)

            } else {
               // Toast.makeText(context, "Permission denied to read storage", Toast.LENGTH_SHORT).show()
                SnackBarUtils.showSnackBar(webView,WebQFields.PERMISSION_DENIED)
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) return
            val result = intent?.data
            mUploadMessage?.onReceiveValue(if (resultCode == Activity.RESULT_OK) arrayOf(result!!) else null)
            mUploadMessage = null
        }
    }

    fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    private fun initWebView() {
        webView.apply {
            setWebChromeClient(MyWebChromeClient(context, this@WebQ))
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            isHorizontalScrollBarEnabled = false
            overScrollMode = WebView.OVER_SCROLL_NEVER
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    private class MyWebChromeClient(private val context: Context, private val webQ: WebQ) : WebChromeClient() {
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            webQ.mUploadMessage = filePathCallback
            webQ.requestStoragePermission()
            return true
        }
    }
}