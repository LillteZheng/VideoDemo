package com.zhengsr.zplayer.ui.controller

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.zhengsr.zplayer.ui.PlayerView
import kotlinx.android.synthetic.main.zplayer_prepare_layout.view.*

/**
 * @author by zhengshaorui 2022/5/29
 * describe：控制器，比如进度条，播放、暂停等
 */
class PlayerController : FrameLayout, PrepareView.PrepareListener {
    companion object {
        private const val TAG = "PlayerController"
    }

    private var playerView: PlayerView? = null
    private var prepareView: PrepareView = PrepareView(this,this)
    private var playingView: PlayingView = PlayingView(this)

    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, desStyle: Int) :
            super(context, attributeSet, desStyle)

    fun setPlayerView(playerView: PlayerView) {
        this.playerView = playerView

    }

    override fun startPlay() {
        playerView?.start()
        playingView.show()
    }


}