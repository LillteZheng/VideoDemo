package com.zhengsr.videodemo.activity.codec;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.media.MyExtractor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DecodeMediaActivity extends AppCompatActivity {
    private static final String TAG = "DecodeMediaActivity";
    private TextureView mTextureView;
    private VideoDecodeSync mVideoSync;
    private AudioDecodeSync mAudioDecodeSync;
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(2);
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
        if (mExecutorService.isShutdown()){
            mExecutorService = Executors.newFixedThreadPool(2);
        }
        mVideoSync = new VideoDecodeSync();
        mAudioDecodeSync = new AudioDecodeSync();
        mExecutorService.execute(mVideoSync);
        mExecutorService.execute(mAudioDecodeSync);
    }

    public void async(View view) {
        if (mVideoSync != null) {
            mVideoSync.done();
        }
        if (mAudioDecodeSync != null) {
            mAudioDecodeSync.done();
        }

        mExecutorService.shutdownNow();

        new AsyncDecode().start();
    }


    class AsyncDecode {
        MediaFormat mediaFormat;
        MediaCodec mediaCodec;
        MyExtractor extractor;

        public AsyncDecode() {
            try {
                extractor = new MyExtractor(Constants.VIDEO_PATH);
                mediaFormat = (extractor.getVideoFormat() );
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                extractor.selectTrack(extractor.getVideoTrackId());
                mediaCodec = MediaCodec.createDecoderByType(mime);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        private void start() {
            mediaCodec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    ByteBuffer inputBuffer = codec.getInputBuffer(index);
                    boolean isDone = false;
                   // while (!isDone) {
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
                            isDone = true;
                        }
                   // }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                    Log.d(TAG, "zsr onOutputBufferAvailable: "+Thread.currentThread());
                    codec.releaseOutputBuffer(index, true);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    codec.reset();
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            mediaCodec.configure(mediaFormat, new Surface(mTextureView.getSurfaceTexture()), null, 0);
            mediaCodec.start();
        }

    }

    /**
     * 解码基类，用于解码音视频
     */
    abstract class BaseDecode implements Runnable {
        final static int VIDEO = 1;
        final static int AUDIO = 2;
        //等待时间
        final static int TIME_US = 1000;
        MediaFormat mediaFormat;
        MediaCodec mediaCodec;
        MyExtractor extractor;

        private boolean isDone;
        public BaseDecode() {
            try {
                //获取 MediaExtractor
                extractor = new MyExtractor(Constants.VIDEO_PATH);
                //判断是音频还是视频
                int type = decodeType();
                //拿到音频或视频的 MediaFormat
                mediaFormat = (type == VIDEO ? extractor.getVideoFormat() : extractor.getAudioFormat());
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                //选择要解析的轨道
                extractor.selectTrack(type == VIDEO ? extractor.getVideoTrackId() : extractor.getAudioTrackId());
                //创建 MediaCodec
                mediaCodec = MediaCodec.createDecoderByType(mime);
                //由子类去配置
                configure();
                //开始工作，进入编解码状态
                mediaCodec.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {


            try {

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                //编码
                while (!isDone) {
                    /**
                     * 延迟 TIME_US 等待拿到空的 input buffer下标，单位为 us
                     * -1 表示一直等待，知道拿到数据
                     */
                    int inputBufferId = mediaCodec.dequeueInputBuffer(TIME_US);

                    if (inputBufferId > 0) {
                        //拿到 可用的，空的 input buffer
                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                        if (inputBuffer != null) {
                            /**
                             * 通过 mediaExtractor.readSampleData(buffer, 0) 拿到视频的当前帧的buffer
                             *
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
                                //结束
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
                    }else{
                        isDone = false;
                    }

                    boolean isFinish =  handleOutputData(info);
                    if (isFinish){
                        break;
                    }

                }

                done();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        protected void done(){
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
        abstract int decodeType();

        abstract void configure();

        abstract boolean handleOutputData(MediaCodec.BufferInfo info);

    }


    class AudioDecodeSync extends BaseDecode {

        private int mPcmEncode;
        //一帧的最小buffer大小
        private final int minBufferSize;
        private AudioTrack audioTrack;

        private boolean isDone;

        public AudioDecodeSync() {
            //mediaFormat.getString(MediaFormat.KEY_BIT_RATE)
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

            int channelConfig = channelCount == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
            minBufferSize = AudioTrack.getMinBufferSize(sampleRate,channelConfig, mPcmEncode);


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


            audioTrack = new AudioTrack(
                    attributes,
                    format,
                    minBufferSize,
                    AudioTrack.MODE_STREAM, //采用流模式
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );

            audioTrack.play();
        }

        @Override
        int decodeType() {
            return AUDIO;
        }

        @Override
        void configure() {
            mediaCodec.configure(mediaFormat, null, null, 0);
        }

        @Override
        boolean handleOutputData(MediaCodec.BufferInfo info) {
            int outputIndex = mediaCodec.dequeueOutputBuffer(info, TIME_US);
            ByteBuffer outputBuffer;
            if (outputIndex >= 0) {
                outputBuffer = mediaCodec.getOutputBuffer(outputIndex);
                audioTrack.write(outputBuffer,info.size,AudioTrack.WRITE_BLOCKING);
                mediaCodec.releaseOutputBuffer(outputIndex, false);
                //outputIndex = mediaCodec.dequeueOutputBuffer(decodeBufferInfo, TIME_US);
            }
            // 在所有解码后的帧都被渲染后，就可以停止播放了
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM");

                return true;
            }
            return false;
        }

        @Override
        protected void done() {
            super.done();
            //释放 AudioTrack
            if (audioTrack.getState()!= AudioTrack.STATE_UNINITIALIZED) {
                audioTrack.stop();
            }
            audioTrack.release();
        }
    }

    class VideoDecodeSync extends BaseDecode {
        // 用于对准视频的时间戳
        private long startMs = System.currentTimeMillis();
        private boolean isDone = false;

        @Override
        int decodeType() {
            return VIDEO;
        }

        @Override
        void configure() {
            mediaCodec.configure(mediaFormat, new Surface(mTextureView.getSurfaceTexture()), null, 0);
        }

        @Override
        boolean handleOutputData(MediaCodec.BufferInfo info) {
            int outputId = mediaCodec.dequeueOutputBuffer(info, TIME_US);

            if (outputId >= 0){
                sleepRender(info, startMs);
                mediaCodec.releaseOutputBuffer(outputId, true);
            }

            // 在所有解码后的帧都被渲染后，就可以停止播放了
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM");

                return true;
            }

            return false;
        }


    }



    /*
     *  数据的时间戳对齐
     */
    private void sleepRender(MediaCodec.BufferInfo info, long startMs) {
        // 这里的时间是 毫秒  presentationTimeUs 的时间是累加的 以微秒进行一帧一帧的累加
        long timeDifference = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startMs);
        if (timeDifference > 0) {

            try {
                Thread.sleep(timeDifference);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
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

    }
}