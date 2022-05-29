package com.zhengsr.playerdemo.player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.midi.MidiDeviceInfo
import android.util.Log
import android.view.Surface
import com.zhengsr.playerdemo.listener.PlayerEventListener

/**
 * @author by zhengshaorui 2022/5/29
 * describe：原生播放器
 */
class AndroidMediaPlayer {
    private val TAG = "AndroidMediaPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var listener: PlayerEventListener? = null



    fun setPlayerEventListener(listener: PlayerEventListener): AndroidMediaPlayer {
        this.listener = listener
        return this
    }

     fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            /*val build = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            setAudioAttributes(build)*/
            setOnPreparedListener(preparedListener)
            setOnVideoSizeChangedListener(sizeChangedListener)
            setOnCompletionListener(completionListener)
            setOnErrorListener(errorListener)
            setOnInfoListener(infoListener)
            setOnBufferingUpdateListener(bufferingUpdateListener)
        }
    }

    fun setAssert(context: Context, name: String) {
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(name)
        mediaPlayer?.setDataSource(
            fileDescriptor.fileDescriptor,
            fileDescriptor.startOffset,
            fileDescriptor.length
        )
        mediaPlayer?.prepareAsync()
    }

    fun setDataSource(block: MediaPlayer.() -> Unit) {
        mediaPlayer?.let(block)
        mediaPlayer?.prepareAsync()
    }


    fun setSurface(surface: Surface){
        mediaPlayer?.setSurface(surface)
    }
    fun isPlaying() = mediaPlayer?.isPlaying?:false
    fun pause() = mediaPlayer?.pause()

    fun start() {
        mediaPlayer?.start()
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {
        return mediaPlayer?.getDuration() ?: 0
    }

    fun stop(){
        mediaPlayer?.stop()
    }
    fun release(){
        mediaPlayer?.apply {
            setOnPreparedListener(null)
            setOnVideoSizeChangedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            setOnInfoListener(null)
            setOnBufferingUpdateListener(null)
            reset()
            release()
        }
        mediaPlayer = null

    }

    //============================================================
    //                  registerListener
    //============================================================
    private val preparedListener = MediaPlayer.OnPreparedListener { mp ->
        Log.d(TAG, "onPrepared() called with: mp = $mp")
        listener?.onPrepared()
    }
    private var sizeChangedListener =
        MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
            Log.d(
                TAG,
                "onVideoSizeChanged() called with: mp = $mp, width = $width, height = $height"
            )
            val videoWidth = mp.videoWidth
            val videoHeight = mp.videoHeight
            if (videoWidth != 0 && videoHeight != 0) {
                listener?.onVideoSizeChanged(width, height)
            }
        }

    private val completionListener = MediaPlayer.OnCompletionListener {
        Log.d(TAG, "onCompletion() ")
        listener?.onCompletion()
    }

    private val infoListener =
        MediaPlayer.OnInfoListener { mp, what, extra ->
            Log.d(TAG, "onInfo() called with: mp = $mp, what = $what, extra = $extra")
            /**
             * Called to indicate an info or a warning.
            Params:
            mp – the MediaPlayer the info pertains to.
            what – the type of info or warning.
            MEDIA_INFO_UNKNOWN
            MEDIA_INFO_VIDEO_TRACK_LAGGING
            MEDIA_INFO_VIDEO_RENDERING_START
            MEDIA_INFO_BUFFERING_START
            MEDIA_INFO_BUFFERING_END
            MEDIA_INFO_NETWORK_BANDWIDTH (703) - bandwidth information is available (as extra kbps)
            MEDIA_INFO_BAD_INTERLEAVING
            MEDIA_INFO_NOT_SEEKABLE
            MEDIA_INFO_METADATA_UPDATE
            MEDIA_INFO_UNSUPPORTED_SUBTITLE
            MEDIA_INFO_SUBTITLE_TIMED_OUT
            extra – an extra code, specific to the info. Typically implementation dependent.
            Returns:
            True if the method handled the info, false if it didn't. Returning false, or not having
            an OnInfoListener at all, will cause the info to be discarded.
             */
            listener?.onInfo(what, extra)

            true
        }

    private val errorListener =
        MediaPlayer.OnErrorListener { mp, framework_err, impl_err ->
            Log.d(
                TAG,
                "onError() : mp = $mp, framework_err = $framework_err, impl_err = $impl_err"
            )
            listener?.onError()
            true
        }


    private val bufferingUpdateListener =
        MediaPlayer.OnBufferingUpdateListener { mp, percent ->
            Log.d(TAG, "onBufferingUpdate() : mp = $mp, percent = $percent")
        }
}