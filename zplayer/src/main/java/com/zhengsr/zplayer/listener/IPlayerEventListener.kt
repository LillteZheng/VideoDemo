package com.zhengsr.zplayer.listener

import com.zhengsr.zplayer.state.PlayerEvent

interface IPlayerEventListener {
        fun onStatusChange(event: PlayerEvent)
    }