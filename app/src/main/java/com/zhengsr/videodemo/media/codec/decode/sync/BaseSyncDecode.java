package com.zhengsr.videodemo.media.codec.decode.sync;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.view.Surface;

import com.zhengsr.videodemo.media.codec.BaseCodec;

import java.nio.ByteBuffer;

/**
 * 解码基类，用于解码音视频
 */
public abstract class BaseSyncDecode extends BaseCodec implements Runnable {
    //等待时间
    protected final static int TIME_US = 10000;
    protected Surface mSurface;
    private boolean isDone;
    private MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();
    public BaseSyncDecode() {
        super();
        //由子类去配置
        configure();
        //开始工作，进入编解码状态
        mediaCodec.start();
    }

    public BaseSyncDecode(SurfaceTexture surfaceTexture) {
        super();
        mSurface = new Surface(surfaceTexture);
        //由子类去配置
        configure();
        //开始工作，进入编解码状态
        mediaCodec.start();
    }

    public BaseSyncDecode(Surface surface){
        mSurface = surface;
        //由子类去配置
        configure();
        //开始工作，进入编解码状态
        mediaCodec.start();
    }

    @Override
    public void run() {


        try {
            //编码
            while (!isDone) {
                /**
                 * 延迟 TIME_US 等待拿到空的 input buffer下标，单位为 us
                 * -1 表示一直等待，知道拿到数据，0 表示立即返回
                 */
                int inputBufferId = mediaCodec.dequeueInputBuffer(TIME_US);

                if (inputBufferId > 0) {
                    //拿到 可用的，空的 input buffer
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                    if (inputBuffer != null) {
                        /**
                         * 通过 mediaExtractor.readSampleData(buffer, 0) 拿到视频的当前帧的buffer
                         * 通过 mediaExtractor.advance() 拿到下一帧
                         */
                        int size = extractor.readBuffer(inputBuffer);
                        //解析数据
                        if (size >= 0) {
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    size,
                                    extractor.getSampleTime(),
                                    extractor.getSampleFlags()
                            );
                        } else {
                            //结束,传递 end-of-stream 标志
                            mediaCodec.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                            isDone = true;

                        }
                    }
                }
                //解码输出交给子类
                boolean isFinish = handleOutputData(mInfo);
                if (isFinish) {
                    break;
                }

            }

            done();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void done() {
        try {
            isDone = true;
            //释放 mediacodec
            mediaCodec.stop();
            mediaCodec.release();

            //释放 MediaExtractor
            extractor.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected abstract boolean handleOutputData(MediaCodec.BufferInfo info);



    protected abstract void configure();


}