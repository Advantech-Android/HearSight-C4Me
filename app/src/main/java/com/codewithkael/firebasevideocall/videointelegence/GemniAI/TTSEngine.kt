package com.codewithkael.firebasevideocall.videointelegence.GemniAI

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import org.w3c.dom.Text
import java.util.Locale

class TTSEngine(private val context: Context):TextToSpeech.OnInitListener

{
    private var textToSpeech:TextToSpeech=TextToSpeech(context,this)
    private var isInitiated=false

    override fun onInit(status: Int)
    {
        if(status==TextToSpeech.SUCCESS){
            val result=textToSpeech.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
            {
                Log.e("TTS", "Language Not supported", )
                Toast.makeText(context, "Language Not supported", Toast.LENGTH_SHORT).show()
            }
            else
            {
                isInitiated=true
            }
        }
        else
        {
            Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    fun speakOut(text:String){
        if(isInitiated)
        {
            textToSpeech.speak(text,TextToSpeech.QUEUE_FLUSH,null,"")
        }
        else
        {
            Toast.makeText(context, "Initialization failed", Toast.LENGTH_SHORT).show()
        }
    }
    fun shutdown(){
        if(textToSpeech!=null){
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }


}