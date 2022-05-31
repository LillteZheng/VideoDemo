package com.zhengsr.zplayer.ui

import android.content.Context
import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.zhengsr.zplayer.player.AndroidMediaPlayer

/**
 * @author by zhengshaorui 2022/5/6
 * describe：SurfaceView 渲染
 */
class SurfaceViewRender : SurfaceView {
    constructor(context: Context) : super(context) {
        holder.setFormat(PixelFormat.RGBA_8888)
    }

    private var player: AndroidMediaPlayer? = null
    fun setPlayer(player: AndroidMediaPlayer) {
        this.player = player
        holder.addCallback(surfaceListener)
    }


    private val surfaceListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            player?.setSurface(holder.surface)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            player?.setSurface(holder.surface)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
        }

    }
    fun release(){
        //surface 会自己释放surfaceholder ，不需要担心
    }
}