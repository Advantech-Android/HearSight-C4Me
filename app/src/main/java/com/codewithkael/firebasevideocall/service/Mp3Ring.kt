package com.codewithkael.firebasevideocall.service
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import com.codewithkael.firebasevideocall.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Mp3Ring @Inject constructor(private val context:Context)
{
    private var mediaPlayer: MediaPlayer? = null

    private val mp3Resource: Int = R.raw.song
    @SuppressLint("SuspiciousIndentation")
    fun startMP3() {
        if (mediaPlayer == null)
            mediaPlayer = MediaPlayer.create(context, mp3Resource)
        mediaPlayer?.start()
    }

    fun stopMP3()
    {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}