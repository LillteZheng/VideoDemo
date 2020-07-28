package com.zhengsr.videodemo.media.codec.decode.async;

import android.graphics.SurfaceTexture;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Message;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author by zhengshaorui 2020/7/27-17:17
 * describe：异步解码
 */
public class AsyncVideoDecode extends BaseAsyncDecode {
    private static final String TAG = "AsyncVideoDecode";
    private Surface mSurface;
    private long mTime = -1;
    private Map<Integer, MediaCodec.BufferInfo> map =
            new ConcurrentHashMap<>();
    public AsyncVideoDecode(SurfaceTexture surfaceTexture) {
        super();
        mSurface = new Surface(surfaceTexture);
    }

    @Override
    public void start(){
        super.start();
        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
                int size = extractor.readBuffer(inputBuffer);
                if (size >= 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            size,
                            extractor.getSampleTime(),
                            extractor.getSampleFlags()
                    );
                    // mHandler.sendEmptyMessage(MSG_VIDEO_INPUT);
                } else {
                    //结束
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                    );
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                Message msg = new Message();
                msg.what = MSG_VIDEO_OUTPUT;
                Bundle bundle = new Bundle();
                bundle.putInt("index",index);
                bundle.putLong("time",info.presentationTimeUs);
                msg.setData(bundle);
                mHandler.sendMessage(msg);


            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                codec.stop();
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

            }
        });
        mediaCodec.configure(mediaFormat,mSurface,null,0);
        mediaCodec.start();


    }

    @Override
    protected int decodeType() {
        return VIDEO;
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what){

            case MSG_VIDEO_OUTPUT:
                try {
                    if (mTime == -1) {
                        mTime = System.currentTimeMillis();
                    }

                    Bundle bundle = msg.getData();
                    int index = bundle.getInt("index");
                    long ptsTime = bundle.getLong("time");
                    sleepRender(ptsTime,mTime);
                    mediaCodec.releaseOutputBuffer(index, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:break;
        }
        return super.handleMessage(msg);
    }




    /**
     * 数据的时间戳对齐
     **/
    private long sleepRender(long ptsTimes, long startMs) {
        /**
         * 注意这里是以 0 为出事目标的，info.presenttationTimes 的单位为微秒
         * 这里用系统时间来模拟两帧的时间差
         */
        ptsTimes = ptsTimes / 1000;
        long systemTimes = System.currentTimeMillis() - startMs;
        long timeDifference = ptsTimes - systemTimes;
        // 如果当前帧比系统时间差快了，则延时以下
        if (timeDifference > 0) {
            try {
                //todo 受系统影响，建议还是用视频本身去告诉解码器 pts 时间
                Thread.sleep(timeDifference);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
        return timeDifference;
    }
}
