package com.zhengsr.zplayer.ui

import android.content.Context
import android.media.MediaPlayer
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.zhengsr.playerdemo.listener.IMediaPlayerListener
import com.zhengsr.zplayer.listener.IPlayerEventListener
import com.zhengsr.zplayer.player.AndroidMediaPlayer
import com.zhengsr.zplayer.state.PlayerEvent
import com.zhengsr.zplayer.ui.controller.ControlWrapper
import com.zhengsr.zplayer.ui.controller.PlayerController

/**
 * @author by zhengshaorui 2022/5/31
 * describe：
 */
open class AbsVideoView: FrameLayout {
    private  val TAG = "AbsVideoView"
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, desStyle: Int) :
            super(context, attributeSet, desStyle) {
        initPlayer()
        addView()
    }
    private var renderView: SurfaceViewRender? = null
    protected var controller: PlayerController? = null
    protected var player: AndroidMediaPlayer? = null


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
        }
        //把controller 放进去
        addView(controller)
        keepScreenOn = true

    }

    private val playerEventListener = object : IMediaPlayerListener {
        override fun onError() {
            Log.d(TAG, "onError() called")
            notityEvent(PlayerEvent.ERROR)
        }

        override fun onCompletion() {
            Log.d(TAG, "onCompletion() called")
            notityEvent(PlayerEvent.COMPLETION)
        }

        override fun onInfo(what: Int, extra: Int) {

            Log.d(TAG, "onInfo() called with: what = $what, extra = $extra")
            when(what){
                MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> notityEvent(PlayerEvent.START)
            }
        }

        override fun onPrepared() {
            Log.d(TAG, "onPrepared() called")
            notityEvent(PlayerEvent.PREPARE)
            // start()
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            Log.d(TAG, "onVideoSizeChanged() called with: width = $width, height = $height")
        }
    }

    private var listener: IPlayerEventListener? = null
    fun addPlayerListener(listener: IPlayerEventListener) {
        this.listener = listener
    }
    private fun notityEvent(event: PlayerEvent){
        listener?.onStatusChange(event)
    }


}