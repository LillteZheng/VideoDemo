package com.zhengsr.zplayer.state

/**
 * @author by zhengshaorui 2022/5/31
 * describe：
 */
enum class PlayerEvent {
    IDEL,
    PREPARE,
    START,
    PLAYING,
    PAUSE,
    RELEASE,
    ERROR,
    COMPLETION
}