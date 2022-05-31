package com.zhengsr.zplayer.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.zhengsr.zplayer.R
import com.zhengsr.zplayer.state.PlayerEvent
import com.zhengsr.zplayer.ui.controller.ControlWrapper
import com.zhengsr.zplayer.ui.controller.PlayerController
import com.zhengsr.zplayer.utils.PlayerUtils
import kotlinx.android.synthetic.main.zplayer_playing_bottom.view.*

/**
 * @author by zhengshaorui 2022/5/30
 * describe：
 */
class BottomPlayerView(context: Context) : AbsPlayerView(context) {
    companion object {
        const val TAG = "BottomPlayerView"
    }

    override fun initView(view: View) {
        super.initView(view)
        dismiss()
    }

    override fun getLayoutId() = R.layout.zplayer_playing_bottom


    override fun onPlayStatusChange(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.START -> {
                iv_play.isSelected = true
                show()
            }
        }
    }

    override fun handleProgress(totalTime: Long, curTime: Long, pos: Int) {
        super.handleProgress(totalTime, curTime, pos)
        view.tv_total_time.text = PlayerUtils.formatTime(totalTime)
        view.tv_curr_time.text = PlayerUtils.formatTime(curTime)
        view.seekBar.progress = pos
    }



}