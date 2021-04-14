package com.zhengsr.videodemo.utils

import android.animation.PropertyValuesHolder
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.rtp.AudioStream
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import com.zhengsr.videodemo.Constants

/**
 * @author by zhengshaorui 2021/4/13 14:15
 * describe：
 */
private const val TAG = "MediaPlayerHelper"
object MediaPlayerHelper {
    var mediaPlayer:MediaPlayer?  = null

    @JvmStatic
    fun prepare(videoPath:String,holder: SurfaceHolder,prepareListener:OnPreparedListener){
        mediaPlayer = MediaPlayer()
        mediaPlayer?.apply {
            try {
                reset()
                setDataSource(videoPath)
                setAudioStreamType(AudioManager.STREAM_MUSIC)
                setDisplay(holder)
                prepareAsync()
                setOnPreparedListener(prepareListener)
            } catch (e: Exception) {
                Log.d(TAG, "zsr play: $e")
            }
        }
    }


    @JvmStatic
    fun play(){
        mediaPlayer?.start()
    }
    @JvmStatic
    fun pause(){
        mediaPlayer?.pause()
    }
    @JvmStatic
    fun release(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

}