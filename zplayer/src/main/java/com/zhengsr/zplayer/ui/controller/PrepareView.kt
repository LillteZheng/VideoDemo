package com.zhengsr.zplayer.ui.controller

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.zhengsr.zplayer.R
import com.zhengsr.zplayer.ui.PlayerView
import kotlinx.android.synthetic.main.zplayer_prepare_layout.view.*
import java.util.zip.GZIPOutputStream

/**
 * @author by zhengshaorui 2022/5/30
 * describe：
 */
class PrepareView {
    constructor(playerController: PlayerController, listener: PrepareListener){
        val prepareView =
            LayoutInflater.from(playerController.context).inflate(R.layout.zplayer_prepare_layout, playerController, false)
        playerController.addView(prepareView)
        prepareView.findViewById<ImageView>(R.id.start_play).setOnClickListener {
           // playerView.start()
            listener.startPlay()
            prepareView.visibility = View.GONE
        }
    }

    interface PrepareListener{
        fun startPlay()
    }
}