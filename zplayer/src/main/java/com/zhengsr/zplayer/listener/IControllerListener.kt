package com.zhengsr.zplayer.listener

/**
 * @author by zhengshaorui 2022/5/31
 * describe：控制器接口
 */
interface IControllerListener {
    /**
     * 开始播放
     */
    fun start()

    /**
     * 暂停播放
     */
    fun pause()

    fun onResume(){}

    /**
     * 获取视频总时长
     */
    fun getDuration(): Long


    /**
     * 获取当前播放的位置
     */
    fun getCurrentPosition(): Long

    /**
     * 调整播放进度
     */
    fun seekTo(pos: Long)

    /**
     * 是否处于播放状态
     */
    fun isPlaying(): Boolean

    /**
     * 获取当前缓冲百分比
     */
    fun getBufferedPercentage(): Int
}