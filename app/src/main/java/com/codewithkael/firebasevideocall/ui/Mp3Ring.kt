package com.codewithkael.firebasevideocall.ui
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import com.codewithkael.firebasevideocall.R
import com.google.android.material.snackbar.Snackbar
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Mp3Ring @Inject constructor(private val context:Context)
{

    private var mediaPlayer: MediaPlayer? = null

    private val mp3ResourceIncoming: Int = R.raw.song//incoming caller tune
    private val mp3ResourceOutgoing:Int=R.raw.ring//outgoing caller tune
    @SuppressLint("SuspiciousIndentation")
    fun startMP3(isIncoming: Boolean) {
        val mp3Resource = if (isIncoming) mp3ResourceIncoming else mp3ResourceOutgoing
    if(mediaPlayer==null)  mediaPlayer = MediaPlayer.create(context, mp3Resource)

        mediaPlayer?.start()


    }
    fun stopMP3()
    {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}