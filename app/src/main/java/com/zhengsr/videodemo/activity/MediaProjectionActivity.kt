package com.zhengsr.videodemo.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zhengsr.videodemo.R
import com.zhengsr.videodemo.activity.mediaproject.MediaProjectService
import com.zhengsr.videodemo.scopeIo
import com.zhengsr.videodemo.utils.CloseUtils
import com.zhengsr.videodemo.utils.MediaPlayerHelper
import com.zhengsr.videodemo.withMain
import kotlinx.android.synthetic.main.activity_media_projection.*
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

fun isAndroidQ(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

class MediaProjectionActivity : AppCompatActivity() {
    private val TAG = "MediaProjectionActivity"
    private val MIRROR_CODE = 2
    private val CAPTURE_CODE = 1
    private lateinit var mediaManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var imageReader: ImageReader? = null
    private var isRecord = false



    private var path = ""
    private val fileName = "mediaprojection.mp4"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_projection)
        if (isAndroidQ()) {
            bindService(
                Intent(this, MediaProjectService::class.java),
                conn, Context.BIND_AUTO_CREATE
            )
        }

    }

    private var binder: MediaProjectService.MyBinder? = null
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as MediaProjectService.MyBinder?
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
        }

    }

    fun capture(view: View) {
        mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaManager.createScreenCaptureIntent().apply {
            startActivityForResult(this, CAPTURE_CODE)
        }
    }

    fun mediaProjecing(view: View) {
        val btn = view as Button
        if (!isRecord) {
            isRecord = true
            mediaManager =
                getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaManager.createScreenCaptureIntent().apply {
                startActivityForResult(this, MIRROR_CODE)
            }
            btn.text = "正在录制，可随意切换界面，点击结束并播放"
        } else {
            try {
                btn.text = "点击开始屏幕录制"
                recorder?.stop()
                mediaProjection?.stop()
                Toast.makeText(this, "开始播放", Toast.LENGTH_SHORT).show()
                val file = File(path, "/mediaprojection.mp4")
                MediaPlayerHelper.prepare(
                    file.absolutePath,
                    surfaceview.holder,
                    MediaPlayer.OnPreparedListener {
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
        if (resultCode == Activity.RESULT_OK) {
            if (isAndroidQ()) {
                binder?.startNotification()
                binder?.registerCallback(requestCode,resultCode,data
                ) { requestCode, mediaProjection ->
                    this.mediaProjection = mediaProjection
                    doFunction(requestCode)
                }
            } else {
                //获取到操作对象
                data?.let {
                    mediaProjection = mediaManager.getMediaProjection(resultCode, it)
                }
               doFunction(requestCode)
            }
        }

    }
    private fun doFunction(requestCode:Int){
        when (requestCode) {
            //截屏
            CAPTURE_CODE -> {
                Log.d(TAG, "开始截屏")
                configImageReader()
            }
            //录屏
            MIRROR_CODE -> {
                if (configMediaRecorder()) {
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

    @SuppressLint("WrongConstant")
    private fun configImageReader() {
        val dm = resources.displayMetrics
        imageReader = ImageReader.newInstance(
            dm.widthPixels, dm.heightPixels,
            PixelFormat.RGBA_8888, 1
        ).apply {
            setOnImageAvailableListener({
                savePicTask(it)
            }, null)
            //把内容投射到ImageReader 的surface
            mediaProjection?.createVirtualDisplay(
                TAG, dm.widthPixels, dm.heightPixels, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null
            )
        }

    }

    /**
     * 配置MediaProjection
     */
    private fun configMediaRecorder(): Boolean {
        path = this.filesDir.absolutePath + "/mediaproject"
        //创建文件夹
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(path, fileName)
        if (file.exists()) {
            file.delete()
        }
        try {
            file.createNewFile()
        } catch (e: Exception) {
            Log.e(TAG, " configMediaRecorder: $e")
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
            setOutputFile(file.absolutePath)
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
        imageReader?.close()
        try {
            File(path,fileName).delete()
            recorder?.stop()
            recorder?.release()
            recorder = null
        } catch (e: Exception) {
        }
        if (isAndroidQ()){
            unbindService(conn)
        }

    }

    /**
     * 保存图片
     */
    private fun savePicTask(reader: ImageReader) {
        scopeIo {
            var image: Image? = null
            try {
                //获取捕获的照片数据
                image = reader.acquireLatestImage()
                val width = image.width
                val height = image.height
                //拿到所有的 Plane 数组
                val planes = image.planes
                val plane = planes[0]

                val buffer: ByteBuffer = plane.buffer
                //相邻像素样本之间的距离，因为RGBA，所以间距是4个字节
                val pixelStride = plane.pixelStride
                //每行的宽度
                val rowStride = plane.rowStride
                //因为内存对齐问题，每个buffer 宽度不同，所以通过pixelStride * width 得到大概的宽度，
                //然后通过 rowStride 去减，得到大概的内存偏移量，不过一般都是对齐的。
                val rowPadding = rowStride - pixelStride * width
                // 创建具体的bitmap大小，由于rowPadding是RGBA 4个通道的，所以也要除以pixelStride，得到实际的宽
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height, Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)

                withMain {
                    val canvas = surfaceview.holder.lockCanvas()
                    with(canvas) {
                        drawBitmap(bitmap, 0f, 0f, null)
                        surfaceview.holder.unlockCanvasAndPost(this)
                    }
                    Toast.makeText(this@MediaProjectionActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    mediaProjection?.stop()
                }
            } catch (e: java.lang.Exception) {
                Log.d(TAG, "zsr doInBackground: $e")
            } finally {
                //记得关闭 image
                try {
                    image?.close()
                } catch (e: Exception) {
                }
            }
        }
    }


}