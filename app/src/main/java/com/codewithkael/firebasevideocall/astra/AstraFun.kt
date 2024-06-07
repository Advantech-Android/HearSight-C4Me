package com.codewithkael.firebasevideocall.astra


import android.util.Log
import com.codewithkael.firebasevideocall.astra.URL._url
import okhttp3.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
// Define the OkHttpClient
val client = OkHttpClient()

const val TAG="==>ara"
object URL {val _url="https://c129-117-197-192-6.ngrok-free.app/"
}
fun startServer() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(_url)  // Change to your Flask server URL
       // .url("https://2738-117-255-116-191.ngrok-free.app/")  // Change to your Flask server URL
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            // Handle the failure
            Log.d(TAG, "onFailure: startServer"+e.message.toString())
        }
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.d(TAG, "onResponse: startServer"+responseData.toString())
                // Handle the successful response
            } else {
                Log.d(TAG, "onResponse: Not Successful")
                // Handle the unsuccessful response
            }
        }
    })
}
fun sendGetRequest(url: String, callback: (String?) -> Unit) {
    val request = Request.Builder()
        .url(url)
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "onFailure: sendGetRequest"+e.message.toString())
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { responseBody ->
                Log.d(TAG, "onResponse: sendGetRequest"+responseBody.toString())
                callback(responseBody)

            } ?: callback(null)
        }
    })
}
fun sendPostRequest(url: String, json: String, callback: (String?) -> Unit) {
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = RequestBody.create(mediaType, json)
    //    val body = json.toRequestBody(mediaType)
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "onFailure:sendPostRequest "+e.message)
            callback(null)
        }
        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { responseBody ->
                callback(responseBody)
                Log.d(TAG, "onResponse: sendPostRequest"+responseBody.toString())
            } ?: callback(null)
        }
    })
}


fun resumeCapture() {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("${_url}resume")  // Change to your Flask server URL
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            Log.d(TAG, "onFailure:resumeCapture"+e.message)
        }
        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.d(TAG, "onResponse: resumeCapture"+responseData.toString())
                // Handle the successful response
            } else {
                Log.d(TAG, "onResponse: resumeCapture Not Successful")
                // Handle the unsuccessful response
            }
        }
    })
}