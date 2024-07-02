//package com.codewithkael.firebasevideocall.videointelegence
//
//import OpenAIRequest
//import OpenAIResponse
//import android.graphics.Bitmap
//import android.util.Base64
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import retrofit2.Response
//import java.io.ByteArrayOutputStream
//
//private const val TAG = "==>>BakingViewModel"
//
//class OpenAIViewModel : ViewModel() {
//
//    private var lastPromptTime: Long = 0
//    private val debouncePeriod = 3000L // 3 seconds debounce period
//
//    // It ensures that prompts are not sent too frequently (debounce logic). It runs in the main thread and uses viewModelScope.launch to start a background coroutine.
//    fun sendPrompt(bitmap: Bitmap?, prompt: String, callback: (String) -> Unit) {
//        val currentTime = System.currentTimeMillis()
//        if (currentTime - lastPromptTime >= debouncePeriod) {
//            lastPromptTime = currentTime
//            viewModelScope.launch(Dispatchers.IO) {
//                bitmap?.let { sendPromptStream(it, prompt, callback) }
//            }
//        }
//    }
//
//    // Tries to send a bitmap and a text prompt to the OpenAI model.
//    private suspend fun sendPromptStream(bitmap: Bitmap, prompt: String, callback: (String) -> Unit) {
//        Log.d(TAG, "sendPromptStream: ")
//        try {
//            val openAIApi = RetrofitInstance.api
//            val imageBase64 = bitmapToBase64(bitmap)
//            val request = OpenAIRequest(
//                model = "dall-e-2",//gpt-3.5-turbo
//                prompt = prompt,
//                max_tokens = 8192,
//                temperature = 0.3f,
//                top_k = 64,
//                top_p = 0.70f,
//                input_image = imageBase64
//            )
//            val response: Response<OpenAIResponse> = openAIApi.generateContent(request)
//
//            if (response.isSuccessful) {
//                val result = response.body()?.choices?.firstOrNull()?.text ?: "No response"
//                callback(result)
//            } else {
//                callback("Error: ${response.errorBody()?.string()}")
//            }
//        } catch (e: Exception) {
//            Log.d(TAG, "sendPrompt: ${e.message}")
//            callback("Please wait your camera is ready to focus")
//        }
//    }
//
//    private fun bitmapToBase64(bitmap: Bitmap): String {
//        val outputStream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
//        return Base64.encode(outputStream.toByteArray(), Base64.DEFAULT).toString(Charsets.UTF_8)
//    }
//}
