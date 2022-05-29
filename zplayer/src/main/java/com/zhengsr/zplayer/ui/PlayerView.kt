package com.zhengsr.zplayer.ui

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zhengsr.playerdemo.listener.PlayerEventListener
import com.zhengsr.playerdemo.player.AndroidMediaPlayer
import kotlinx.android.synthetic.main.activity_media_player.view.*

/**
 * @author by zhengshaorui 2022/5/6
 * describe：对外暴露的View，保护SurfaceView，TextureView，控制器等功能
 */
class PlayerView : FrameLayout {
    companion object {
        private const val TAG = "PlayerView"
    }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, desStyle: Int) :
            super(context, attributeSet, desStyle) {
        initPlayer()
        addView()
    }


    private var renderView: SurfaceViewRender? = null
    private var controller: PlayerController? = null
    private var player: AndroidMediaPlayer? = null


    private fun initPlayer() {
        player = AndroidMediaPlayer()
            .apply {
                initMediaPlayer()
                setPlayerEventListener(playerEventListener)
            }
    }

    private fun addView() {
        if (renderView == null) {
            removeView(renderView)
            renderView?.release()
        }
        //初始化 surfaceView
        renderView = SurfaceViewRender(context).apply {
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            layoutParams = params
            //设置播放器到 SurfaceView
            player?.let {
                setPlayer(it)
            }
        }
        //把surfaceView 放进去
        addView(renderView)

        //添加控制器
        if (controller == null) {
            removeView(controller)
        }
        controller = PlayerController(context).apply {
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
            layoutParams = params
            setPlayerView(this@PlayerView)
        }
        //把surfaceView 放进去
        addView(controller)
        keepScreenOn = true

    }

    fun setAssert(name: String){
        player!!.setAssert(context, name)
    }

    fun onResume() {
       /* player?.let {
            if (!it.isPlaying()) {
                it.start()
            }
        }*/
        keepScreenOn = true
    }

    fun onPause() {
       /* player?.let {
            if (it.isPlaying()) {
                it.pause()
            }
        }*/
        keepScreenOn = false
    }

    fun onRelease() {
        player?.release()
    }

    fun start() {
        player?.start()
    }



    private val playerEventListener = object : PlayerEventListener {
        override fun onError() {
            Log.d(TAG, "onError() called")
        }

        override fun onCompletion() {
            Log.d(TAG, "onCompletion() called")
        }

        override fun onInfo(what: Int, extra: Int) {
            Log.d(TAG, "onInfo() called with: what = $what, extra = $extra")
        }

        override fun onPrepared() {
            Log.d(TAG, "onPrepared() called")
           // start()
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            Log.d(TAG, "onVideoSizeChanged() called with: width = $width, height = $height")
        }

    }

}