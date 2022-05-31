package com.zhengsr.zplayer.ui.view

import android.content.Context
import android.view.View
import com.zhengsr.zplayer.R
import com.zhengsr.zplayer.state.PlayerEvent
import com.zhengsr.zplayer.ui.controller.ControlWrapper
import com.zhengsr.zplayer.ui.controller.PlayerController
import kotlinx.android.synthetic.main.zplayer_prepare_layout.view.*

/**
 * @author by zhengshaorui 2022/5/30
 * describe：
 */
class PreparePlayerView(context: Context) : AbsPlayerView(context) {
    companion object{
         const val TAG = "PreparePlayerView"
    }


    override fun initView(view: View) {
        super.initView(view)
        view.start_play.setOnClickListener {
            controller?.start()
            dismiss()
        }
    }
    override fun getLayoutId() = R.layout.zplayer_prepare_layout



}