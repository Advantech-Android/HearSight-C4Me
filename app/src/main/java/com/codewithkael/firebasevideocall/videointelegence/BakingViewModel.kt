package com.codewithkael.firebasevideocall.videointelegence

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "==>>BakingViewModel"

class BakingViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "AIzaSyDbxtup1y6wvj9o8G16ZEj0cRTzmZO9yaA",
        generationConfig {
            temperature = 0.3f
            topK = 64
            topP = 0.70f
            maxOutputTokens = 8192

        }
    )

    private var lastPromptTime: Long = 0
    private val debouncePeriod = 3000L // 3 seconds debounce period

    // It ensures that prompts are not sent too frequently (debounce logic). It runs in the main thread and uses viewModelScope.launch to start a background coroutine.
    fun sendPrompt(bitmap: Bitmap, prompt: String,callback:(String)->Unit)//image,prompt and AI respose input
    {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPromptTime >= debouncePeriod) {
            lastPromptTime = currentTime
            // sendPromptInternal(bitmap, prompt)
            viewModelScope.launch(Dispatchers.IO) {
                sendPromptStream(bitmap, prompt,callback)
            }
        }
    }

    private fun sendPromptInternal(bitmap: Bitmap, prompt: String) {
        Log.d(TAG, "sendPrompt: $bitmap")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
                response.text?.let { outputContent ->
                    Log.d(TAG, "sendPrompt: $outputContent")
                }
            } catch (e: Exception) {
                Log.d(TAG, "sendPrompt: ${e.message}")
            }
        }
    }

    //Tries to send a bitmap and a text prompt to a generative AI model.
    private suspend fun sendPromptStream(bitmap: Bitmap, prompt: String,callback:(String)->Unit)
    {
        Log.d(TAG, "sendPromptStream: ")
        try
        {
            val inputContent = content {//content is a builder it adds the bitmap-image and prompt-text to the content
                image(bitmap)
                text(prompt)
            }
            var fullResponse = ""//to accumulate the response chunks().

            generativeModel.generateContentStream(inputContent).collect { chunk ->//This method returns a flow of response chunks.
                Log.d(TAG, "sendPromptStream: --->${chunk.text}")
                fullResponse = chunk.text.toString()// Updates fullResponse with the text of the current chunk.
            }
            val lines=fullResponse.split("\n").take(2)
            val first2lines=lines.joinToString("\n")

            callback(first2lines)
        }
        catch (e: Exception)
        {
            Log.d(TAG, "sendPrompt: ${e.message}")
            callback("Please wait your camera is ready to focus")
        }
    }
}
