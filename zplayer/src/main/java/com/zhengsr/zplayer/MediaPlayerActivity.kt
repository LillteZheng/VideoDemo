package com.zhengsr.zplayer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.zhengsr.playerdemo.listener.PlayerEventListener
import com.zhengsr.playerdemo.player.AndroidMediaPlayer
import kotlinx.android.synthetic.main.activity_media_player.*

class MediaPlayerActivity : AppCompatActivity() {
    private val TAG = "MediaPlayerActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_player)
        playerView.setAssert("任然 - 飞鸟和蝉.mp3")
    }


    override fun onResume() {
        super.onResume()
        playerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        playerView.onPause()
    }

    fun playAudio(view: View) {
        //use raw
        /*player?.setDataSource {
            val fileDescriptor = this@MediaPlayerActivity.assets.openFd("任然 - 飞鸟和蝉.mp3")
            setDataSource(
                fileDescriptor.fileDescriptor,
                fileDescriptor.startOffset,
                fileDescriptor.length
            )
        }*/

    }



    override fun onDestroy() {
        super.onDestroy()
        playerView.onRelease()
    }


}