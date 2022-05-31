package com.zhengsr.zplayer.ui.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.zhengsr.zplayer.R
import com.zhengsr.zplayer.state.PlayerEvent
import com.zhengsr.zplayer.ui.controller.ControlWrapper

/**
 * @author by zhengshaorui 2022/5/31
 * describe：
 */
abstract class AbsPlayerView(context: Context) : FrameLayout(context) {
    private val TAG = "AbsPlayerView"
    protected var controller: ControlWrapper? = null
    protected lateinit var view: View

    init {
        view =
            LayoutInflater.from(context).inflate(getLayoutId(), this, false)
        Log.d(TAG, "zsr : $view")
        addView(view)
        initView(view)
    }


    open fun initView(view: View) {

    }

    open fun show() {
        visibility = View.VISIBLE
    }

    open fun dismiss() {
        visibility = View.GONE
    }

    abstract fun getLayoutId(): Int

    fun attach(controlWrapper: ControlWrapper) {
        this.controller = controlWrapper
    }

    open fun onPlayStatusChange(playerEvent: PlayerEvent) {}

    open fun handleProgress(totalTime: Long, curTime: Long, pos: Int) {}

}