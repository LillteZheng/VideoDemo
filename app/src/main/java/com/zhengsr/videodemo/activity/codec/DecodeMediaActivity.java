package com.zhengsr.videodemo.activity.codec;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.media.MyExtractor;
import com.zhengsr.videodemo.media.codec.decode.async.AsyncAudioDecode;
import com.zhengsr.videodemo.media.codec.decode.async.AsyncVideoDecode;
import com.zhengsr.videodemo.media.codec.decode.sync.SyncAudioDecode;
import com.zhengsr.videodemo.media.codec.decode.sync.SyncVideoDecode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class DecodeMediaActivity extends AppCompatActivity {
    private static final String TAG = "DecodeMediaActivity";
    private TextureView mTextureView;
    private SyncVideoDecode mVideoSync;
    private SyncAudioDecode mAudioDecodeSync;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(2);
    private Handler handler;
    private BlockingQueue<Integer> mBlockingQueue = new LinkedBlockingDeque<>();
    private BlockingQueue<Integer> mAudioBlockingQueue = new LinkedBlockingDeque<>();
    private AsyncVideoDecode asyncVideoDecode;
    private AsyncVideoDecode mAsyncVide;
    private AsyncAudioDecode mAsyncAudio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decode_media);
        init();


    }


    /**
     * 配置TextureView
     */
    private void init() {
        mTextureView = findViewById(R.id.surface);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {


            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                MyExtractor myExtractor = new MyExtractor(Constants.VIDEO_PATH);
                MediaFormat videoFormat = myExtractor.getVideoFormat();
                int vw = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
                int vh = videoFormat.getInteger(MediaFormat.KEY_WIDTH);
                ViewGroup.LayoutParams params = mTextureView.getLayoutParams();
                params.width = vw;
                params.height = vh;
                mTextureView.setLayoutParams(params);

                mVideoSync = new SyncVideoDecode(surface);
                mAudioDecodeSync = new SyncAudioDecode();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    public void sync(View view) {
        if (mExecutorService.isShutdown()) {
            mExecutorService = Executors.newFixedThreadPool(2);
        }

        if (mVideoSync != null) {
            mVideoSync.done();
        }
        if (mAudioDecodeSync != null) {
            mAudioDecodeSync.done();
        }

        if (mAsyncAudio != null) {
            mAsyncAudio.stop();
        }
        if (mAsyncVide != null) {
            mAsyncVide.stop();
        }


        mExecutorService.execute(mVideoSync);
        mExecutorService.execute(mAudioDecodeSync);
    }

    public void async(View view) {


        if (mAsyncAudio != null) {
            mAsyncAudio.release();
        }
        if (mAsyncVide != null) {
            mAsyncVide.release();
        }

       // mExecutorService.shutdownNow();

        mAsyncVide = new AsyncVideoDecode(mTextureView.getSurfaceTexture());

        mAsyncVide.start();

        mAsyncAudio = new AsyncAudioDecode();
        mAsyncAudio.start();

    }








    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoSync != null) {
            mVideoSync.done();
        }
        if (mAudioDecodeSync != null) {
            mAudioDecodeSync.done();
        }
        mExecutorService.shutdown();

        if (mAsyncVide != null) {
            mAsyncAudio.release();
        }
        if (mAsyncAudio != null) {
            mAsyncAudio.release();
        }
    }
}