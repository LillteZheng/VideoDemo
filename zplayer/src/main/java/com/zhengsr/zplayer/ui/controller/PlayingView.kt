package com.zhengsr.zplayer.ui.controller

import android.view.LayoutInflater
import android.view.View
import com.zhengsr.zplayer.R
import com.zhengsr.zplayer.ui.PlayerView

/**
 * @author by zhengshaorui 2022/5/30
 * describe：
 */
class PlayingView {
    private lateinit var view: View
    constructor(playerController: PlayerController){
        view =
            LayoutInflater.from(playerController.context).inflate(R.layout.zplayer_playing_bottom, playerController, false)
        playerController.addView(view)
        view.visibility = View.GONE
    }


    fun show(){
        view.visibility = View.VISIBLE
    }

}