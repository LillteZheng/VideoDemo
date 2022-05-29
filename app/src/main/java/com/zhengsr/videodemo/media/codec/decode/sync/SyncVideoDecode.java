package com.zhengsr.videodemo.media.codec.decode.sync;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

/**
 * 视频解码
 */
public class SyncVideoDecode extends BaseSyncDecode {
    private static final String TAG = "VideoDecodeSync";
    // 用于对准视频的时间戳
    private long mStartMs = -1;

    public SyncVideoDecode(SurfaceTexture surfaceTexture) {
        super(surfaceTexture);

    }
    public SyncVideoDecode(Surface surface){
        super(surface);
    }

    @Override
    public int decodeType() {
        return VIDEO;
    }

    @Override
    protected void configure() {
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH,4096);
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT,2304);
        mediaCodec.configure(mediaFormat, mSurface, null, 0);
    }

    @Override
    protected boolean handleOutputData(MediaCodec.BufferInfo info) {
        //等到拿到输出的buffer下标
        int outputId = mediaCodec.dequeueOutputBuffer(info, TIME_US);

        if (mStartMs == -1) {
            mStartMs = System.currentTimeMillis();
        }
        while (outputId >= 0) {
            //矫正pts
            sleepRender(info, mStartMs);

            //释放buffer，并渲染到 Surface 中
            mediaCodec.releaseOutputBuffer(outputId, true);


            // 在所有解码后的帧都被渲染后，就可以停止播放了
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM");

                return true;
            }
            Log.d(TAG, "zsr handleOutputData: ");
            outputId = mediaCodec.dequeueOutputBuffer(info, TIME_US);
        }

        return false;
    }


    /**
     * 数据的时间戳对齐
     **/
    private void sleepRender(MediaCodec.BufferInfo info, long startMs) {
        /**
         * 注意这里是以 0 为出事目标的，info.presenttationTimes 的单位为微秒
         * 这里用系统时间来模拟两帧的时间差
         */
        long ptsTimes = info.presentationTimeUs / 1000;
        long systemTimes = System.currentTimeMillis() - startMs;
        long timeDifference = ptsTimes - systemTimes;
        // 如果当前帧比系统时间差快了，则延时以下
        if (timeDifference > 0) {
            try {
                Thread.sleep(timeDifference);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}