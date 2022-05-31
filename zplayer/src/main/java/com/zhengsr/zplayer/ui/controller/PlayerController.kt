package com.zhengsr.zplayer.ui.controller

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import com.zhengsr.zplayer.listener.IPlayerEventListener
import com.zhengsr.zplayer.state.PlayerEvent
import com.zhengsr.zplayer.ui.view.AbsPlayerView
import com.zhengsr.zplayer.ui.view.BottomPlayerView
import com.zhengsr.zplayer.ui.view.PreparePlayerView

/**
 * @author by zhengshaorui 2022/5/29
 * describe：控制器，比如进度条，播放、暂停等
 */
class PlayerController(context: Context) : FrameLayout(context) {
    companion object {
        private const val TAG = "PlayerController"
    }


    private val viewList = mutableMapOf<String, AbsPlayerView>()

    private var controlWrapper: ControlWrapper? = null
    fun setControlWrapper(controlWrapper: ControlWrapper) {
        this.controlWrapper = controlWrapper
        controlWrapper.addEventListener(eventListener)
        for (entry in viewList.entries) {
            addView(entry.value)
            entry.value.attach(controlWrapper)
        }

    }


    private val eventListener = object : IPlayerEventListener {
        override fun onStatusChange(event: PlayerEvent) {
            Log.d(TAG, "onStatusChange() called with: event = $event")
            if (event == PlayerEvent.START){
                post(progressRunnable)
            }
            if (event == PlayerEvent.COMPLETION || event == PlayerEvent.ERROR){
                removeCallbacks(progressRunnable)
            }
            for (entry in viewList.entries) {
                entry.value.onPlayStatusChange(event)
            }

        }

    }

    init {
        viewList[PreparePlayerView.TAG] = PreparePlayerView(context)
        viewList[BottomPlayerView.TAG] = BottomPlayerView(context)
    }

    /**
     * 刷新进度Runnable
     */
    private val progressRunnable: Runnable = object : Runnable {
        override fun run() {
            controlWrapper?.let {
                val pos = handleProgress(it)
                //todo 后续可能有快进，快退
                val speed = 1;
                postDelayed(this, ((1000 - pos % 1000) / speed).toLong())
            }
        }
    }

    private fun handleProgress(controlWrapper: ControlWrapper):Long{
        val duration = controlWrapper.getDuration()
        val position = controlWrapper.getCurrentPosition()
        val pos = (position * 1.0f / duration * 100).toInt()
        Log.d(TAG, "zsr handleProgress: $duration $position $pos")
        for (entry in viewList.entries) {
            entry.value.handleProgress(duration,position,pos)
        }
        return position
    }


}