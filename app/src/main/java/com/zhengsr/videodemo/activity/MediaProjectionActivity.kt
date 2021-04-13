package com.zhengsr.videodemo.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.videodemo.R
import com.zhengsr.videodemo.scopeIo
import com.zhengsr.videodemo.utils.MediaPlayerHelper
import kotlinx.android.synthetic.main.activity_media_projection.*
import java.io.File
import java.io.IOException

class MediaProjectionActivity : AppCompatActivity() {
    private val TAG = "MediaProjectionActivity"
    private lateinit var mediaManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    var recorder :MediaRecorder? = null
    val path = Environment.getExternalStorageDirectory().absolutePath+"/mediaprojection.mp4"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_projection)

      
    }

    var isRecord = false

    fun mediaProjecing(view: View) {
        val btn = view as Button
        if (!isRecord) {
            isRecord = true
            mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaManager.createScreenCaptureIntent().apply {
                startActivityForResult(intent, 2)
            }

            btn.text = "正在录制，可随意切换界面，点击结束并播放"
        } else {
            try {
                btn.text = "点击开始屏幕录制"
                recorder?.stop()
                Log.d(TAG, "开始播放")
                MediaPlayerHelper.prepare(path,surfaceview.holder, MediaPlayer.OnPreparedListener {
                    Log.d(TAG, "zsr onPrepared: ${it.isPlaying}")
                    MediaPlayerHelper.play()
                })

            } catch (e: Exception) {
                Log.d(TAG, "zsr mediaProjecing: $e")
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == Activity.RESULT_OK) {
            data?.let {
                //获取到操作对象
                mediaProjection = mediaManager.getMediaProjection(resultCode, it)
                if (getMediaRecorder()){
                    try {
                        recorder?.start()
                    } catch (e: Exception) {
                        Log.d(TAG, "recorder start: $e")
                    }
                    Toast.makeText(this, "正在录制", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun getMediaRecorder(): Boolean {

        //创建文件夹
        val dir = File(path)
        if (dir.exists()) {
            dir.delete()
        }

        val dm = resources.displayMetrics
        recorder = MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC) //音频载体
            setVideoSource(MediaRecorder.VideoSource.SURFACE) //视频载体
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //输出格式
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB) //音频格式
            setVideoEncoder(MediaRecorder.VideoEncoder.H264) //视频格式
            setVideoSize(dm.widthPixels, dm.heightPixels) //size
            setVideoFrameRate(30) //帧率
            setVideoEncodingBitRate(3 * 1024 * 1024) //比特率
            //设置文件位置
            setOutputFile(dir.absolutePath)
            try {
                prepare()
                virtualDisplay = mediaProjection?.createVirtualDisplay(
                        TAG,
                        dm.widthPixels,
                        dm.heightPixels,
                        dm.densityDpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        surface,
                        null,
                        null
                )
            } catch (e: Exception) {
                Log.e(TAG, "MediaRecord prepare fail : $e")
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        MediaPlayerHelper.release()
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
        } catch (e: Exception) {
        }

    }

}