package com.zhengsr.videodemo.media.codec.decode.async;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author by zhengshaorui 2020/7/27 17:36
 * describe：
 */
public class AsyncAudioDecode extends BaseAsyncDecode {
    private static final String TAG = "AsyncAudioDecode";
    private int mPcmEncode;
    //一帧的最小buffer大小
    private  int mMinBufferSize;
    private AudioTrack mAudioTrack;

    public AsyncAudioDecode() {
        super();
        //拿到采样率
        if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            mPcmEncode = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
        } else {
            //默认采样率为 16bit
            mPcmEncode = AudioFormat.ENCODING_PCM_16BIT;
        }

        //音频采样率
        int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        //获取视频通道数
        int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

        //拿到声道
        int channelConfig = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
        mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, mPcmEncode);


        /**
         * 设置音频信息属性
         * 1.设置支持多媒体属性，比如audio，video
         * 2.设置音频格式，比如 music
         */
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        /**
         * 设置音频数据
         * 1. 设置采样率
         * 2. 设置采样位数
         * 3. 设置声道
         */
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig)
                .build();


        //配置 audioTrack,采用流模式
        mAudioTrack = new AudioTrack(
                attributes,
                format,
                mMinBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
        );
        //监听播放
        mAudioTrack.play();
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
                msg.what = MSG_AUDIO_OUTPUT;
                msg.arg1 = index;
                msg.arg2 = info.size;
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
        mediaCodec.configure(mediaFormat, null, null, 0);
        mediaCodec.start();
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what){

            case MSG_AUDIO_OUTPUT:
                int index = msg.arg1;
                int size = msg.arg2;
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(index);
                try {
                    if (outputBuffer != null) {
                        mAudioTrack.write(outputBuffer, size,AudioTrack.WRITE_BLOCKING);
                        mediaCodec.releaseOutputBuffer(index, false);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            default:break;
        }
        return super.handleMessage(msg);
    }


    @Override
    public void stop() {
        super.stop();
        //释放 AudioTrack
        if (mAudioTrack.getState() != AudioTrack.STATE_UNINITIALIZED) {
            mAudioTrack.stop();
        }
    }

    @Override
    public void release() {
        super.release();
        mAudioTrack.release();
    }

    @Override
    protected int decodeType() {
        return AUDIO;
    }
}
