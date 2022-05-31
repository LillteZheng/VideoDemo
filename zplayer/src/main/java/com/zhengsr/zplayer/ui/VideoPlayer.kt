package com.zhengsr.zplayer.ui

import android.content.Context
import android.util.AttributeSet
import com.zhengsr.zplayer.listener.IControllerListener
import com.zhengsr.zplayer.ui.controller.ControlWrapper

/**
 * @author by zhengshaorui 2022/5/6
 * describe：对外暴露的View，保护SurfaceView，TextureView，控制器等功能
 */
class VideoPlayer : AbsVideoView, IControllerListener {
    companion object {
        private const val TAG = "PlayerView"
    }

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, desStyle: Int) :
            super(context, attributeSet, desStyle) {
        controller?.setControlWrapper(ControlWrapper(this))
    }


    fun setAssert(name: String) {
        player!!.setAssert(context, name)
    }

    override fun onResume() {
        keepScreenOn = true
    }

    override fun getDuration(): Long = player?.getDuration() ?: 0L

    override fun getCurrentPosition() = player?.getCurrentPosition() ?: 0L

    override fun seekTo(pos: Long) {
        val seek = if (pos < 0) {
            0
        } else {
            pos
        }
        player?.seekTo(seek)
    }

    override fun isPlaying() = player?.isPlaying() ?: false

    override fun getBufferedPercentage() = 0

    override fun pause() {
        keepScreenOn = false
    }

    fun onRelease() {
        player?.release()
    }

    override fun start() {
        player?.start()
    }


}