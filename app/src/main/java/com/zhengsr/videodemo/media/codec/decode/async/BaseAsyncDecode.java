package com.zhengsr.videodemo.media.codec.decode.async;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.zhengsr.videodemo.media.codec.BaseCodec;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public abstract class BaseAsyncDecode extends BaseCodec implements Handler.Callback {
    private static final String TAG = "BaseAsyncDecode";
    protected final static int MSG_AUDIO_INPUT = 0X010;
    protected final static int MSG_AUDIO_OUTPUT = 0X011;
    protected final static int MSG_VIDEO_INPUT = 0X012;
    protected final static int MSG_VIDEO_OUTPUT = 0X013;
    protected HandlerThread mHandlerThread;
    protected Handler mHandler;
    protected BlockingQueue<Integer> mBlockingQueue = new LinkedBlockingDeque<>();

    public BaseAsyncDecode() {
        super();
        /**
         * 用 HandlerThread 其实不怎么好，待后面优化
         */
        mHandlerThread = new HandlerThread(decodeType() == VIDEO ? "videoThread" : "audioThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), this);
    }


    public void start() {
        if (mBlockingQueue == null) {
            mBlockingQueue = new LinkedBlockingDeque<>();
        }
        mBlockingQueue.clear();
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread(decodeType() == VIDEO ? "videoThread" : "audioThread");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper(), this);

        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        return false;
    }

    private void releaseMedia() {
        mediaCodec.release();

        //释放 MediaExtractor
        extractor.release();

    }

    private void stopMedia(){
        mediaCodec.stop();
    }

    public void release(){
        try {
            stop();
            releaseMedia();
            mBlockingQueue = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        if (mHandler != null) {
            mHandler.removeMessages(MSG_VIDEO_INPUT);
            mHandler.removeMessages(MSG_VIDEO_OUTPUT);
            mHandler.removeMessages(MSG_AUDIO_INPUT);
            mHandler.removeMessages(MSG_AUDIO_OUTPUT);
            mHandler = null;
        }
        if (mBlockingQueue != null) {
            mBlockingQueue.clear();
        }
        stopMedia();

    }
}
