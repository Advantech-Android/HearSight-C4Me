package com.codewithkael.firebasevideocall.astra

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ActivityAstraDemoBinding


class AstraDemo : AppCompatActivity() {
    var view:ActivityAstraDemoBinding?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view=ActivityAstraDemoBinding.inflate(layoutInflater)
        setContentView(view?.root)
        // Example GET request to stop the Flask server
        view?.getrequest?.setOnClickListener {
            stopServer()
        }

        // Example POST request to set capture interval
        view?.postrequest?.setOnClickListener {
            postFcapture_interval()
        }

        //Change to your Flask server URL
         view?.resume1?.setOnClickListener {
             resumeCapture()
        }
        view?.start?.setOnClickListener {
            startServer()
        }



    }


    private fun stopServer() {
        //val url = "http://localhost:5001/stop"
        val url = "${URL._url}stop"
        sendGetRequest(url) { response ->
            runOnUiThread {
                response?.let {
                    Toast.makeText(this, "Response: $it", Toast.LENGTH_LONG).show()
                } ?: Toast.makeText(this, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun postFcapture_interval(){
        val setIntervalUrl = "${URL._url}set_interval"
        val jsonBody = """{"interval": 5}"""
        sendPostRequest(setIntervalUrl, jsonBody) { response ->
            runOnUiThread {
                response?.let {
                    Toast.makeText(this, "Response: $it", Toast.LENGTH_LONG).show()
                } ?: Toast.makeText(this, "Failed to connect to the server", Toast.LENGTH_LONG).show()
            }
        }
    }


}