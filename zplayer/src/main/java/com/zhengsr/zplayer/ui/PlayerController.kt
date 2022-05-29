package com.zhengsr.zplayer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.zhengsr.zplayer.R
import kotlinx.android.synthetic.main.zplayer_prepare_layout.view.*

/**
 * @author by zhengshaorui 2022/5/29
 * describe：控制器，比如进度条，播放、暂停等
 */
class PlayerController : FrameLayout, View.OnClickListener {
    companion object {
        private const val TAG = "PlayerController"
    }

    private var playerView: PlayerView? = null
    private var prepareView: View? = null

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, desStyle: Int) :
            super(context, attributeSet, desStyle) {
        prepareView =
            LayoutInflater.from(context).inflate(R.layout.zplayer_prepare_layout, this, false)
        addView(prepareView)

        start_play.setOnClickListener(this)
    }

    fun setPlayerView(playerView: PlayerView) {
        this.playerView = playerView
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.start_play -> {
                playerView?.start()
                prepareView?.visibility = View.GONE
            }
        }
    }
}