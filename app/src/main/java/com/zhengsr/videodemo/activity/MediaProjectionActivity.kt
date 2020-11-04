package com.zhengsr.videodemo.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import com.zhengsr.videodemo.R

class MediaProjectionActivity : AppCompatActivity() {
    private val TAG = "MediaProjectionActivity"
    private lateinit var mediaManager: MediaProjectionManager
    private lateinit var surface: Surface
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_projection)
        val textureView: TextureView = findViewById(R.id.surface)
        mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaManager.createScreenCaptureIntent()
        startActivityForResult(intent, 2)

        textureView.post {
            val offset = resources.displayMetrics.widthPixels.toFloat() /
                    resources.displayMetrics.heightPixels
            surfaceHeight = textureView.height
            surfaceWidth = (surfaceHeight * offset).toInt()
            surface = Surface(textureView.surfaceTexture)
        }

        val mediaCodec = MediaCodec.createDecoderByType("video/avc")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "zsr onResume: ")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "zsr onPause: ")
        virtualDisplay?.let {
            it.release()
        }
    }

    fun capture(view: View) {

    }

    fun captureScreen(view: View) {
        virtualDisplay = mediaProjection?.createVirtualDisplay(
                TAG,
                surfaceWidth,
                surfaceHeight,
                resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null
        )
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            data?.let {
                mediaProjection = mediaManager.getMediaProjection(resultCode, it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
    }

}