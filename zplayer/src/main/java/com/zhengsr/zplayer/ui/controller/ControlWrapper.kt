package com.zhengsr.zplayer.ui.controller

import androidx.core.view.get
import com.zhengsr.zplayer.listener.IControllerListener
import com.zhengsr.zplayer.listener.IPlayerEventListener
import com.zhengsr.zplayer.ui.VideoPlayer

/**
 * @author by zhengshaorui 2022/5/31
 * describe：控制包装器
 */
class ControlWrapper(private val videoPlayer: VideoPlayer):IControllerListener {

    fun addEventListener(eventListener: IPlayerEventListener){
        videoPlayer.addPlayerListener(eventListener)
    }

    override fun start() {
        videoPlayer.start()
    }

    override fun pause() {
        videoPlayer.pause()
    }

    override fun getDuration() = videoPlayer.getDuration()

    override fun getCurrentPosition() = videoPlayer.getCurrentPosition()

    override fun seekTo(pos: Long) = videoPlayer.seekTo(pos)

    override fun isPlaying() = videoPlayer.isPlaying()

    override fun getBufferedPercentage() = videoPlayer.getBufferedPercentage()


}