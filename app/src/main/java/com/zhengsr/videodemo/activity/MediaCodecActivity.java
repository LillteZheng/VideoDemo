package com.zhengsr.videodemo.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaCodecActivity extends AppCompatActivity {
    private static final String TAG = "MediaCodecActivity";
    private String mMimeType;
    private MediaFormat mVideoFormat;
    private int mVideoTrackIndex = -1;
    private int mWidth = 720;
    private int mHeight = 1280;
    private MediaExtractor mMediaExtractor;
    private TextureView mTextureView;
    private MediaCodec mDecoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        initTractor();
        init();
    }

    private void initTractor() {


        //通过 MediaExtractor 解析视频
        try {
            mMediaExtractor = new MediaExtractor();
            //设置视频路径
            mMediaExtractor.setDataSource(Constants.VIDEO_PATH);
            //获取轨道数
            int trackCount = mMediaExtractor.getTrackCount();
            Log.d(TAG, "zsr initTractor: " + trackCount);
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "zsr onSurfaceTextureAvailable: " + mime + " " + i);
                //这里只截取视频轨
                if ("video/mp4v-es".equals(mime)) {
                    mVideoFormat = trackFormat;
                    mVideoTrackIndex = i;
                    mMimeType = mime;
                    break;
                }
            }

            mWidth = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
            mHeight = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);
            //设置一下播放控件的宽高
           /* mTextureView.post(new Runnable() {
                @Override
                public void run() {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mWidth, mHeight);
                    mTextureView.setLayoutParams(params);
                }
            });*/


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        mTextureView = findViewById(R.id.surface);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                //   new SyncCodec().start();
                new AsyncCode().start();
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


    /**
     * 同步解析视频
     */
    class SyncCodec extends Thread {

        boolean isDone = false;

        @Override
        public void run() {
            super.run();

            try {
                mMediaExtractor.selectTrack(mVideoTrackIndex);
                mDecoder = MediaCodec.createDecoderByType(mMimeType);
                mDecoder.configure(mVideoFormat, new Surface(mTextureView.getSurfaceTexture()), null, 0);
                //进入 running 状态
                mDecoder.start();

                // 用于对准视频的时间戳
                long startMs = System.currentTimeMillis();
                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                //编码
                while (!isDone) {
                    //单位是 us
                    int inputBufferId = mDecoder.dequeueInputBuffer(10000);
                    if (inputBufferId > 0) {
                        ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputBufferId);
                        if (inputBuffer != null) {
                            int size = mMediaExtractor.readSampleData(inputBuffer, 0);
                            //解析数据
                            if (size >= 0) {
                                mDecoder.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        size,
                                        mMediaExtractor.getSampleTime(),
                                        mMediaExtractor.getSampleFlags()
                                );
                                //取下一帧
                                mMediaExtractor.advance();
                            } else {
                                //结束
                                mDecoder.queueInputBuffer(
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


                    int outputId = mDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outputId) {
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            // 当buffer的格式发生改变，须指向新的buffer格式
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                            break;

                        default:

                            sleepRender(info, startMs);
                            mDecoder.releaseOutputBuffer(outputId, true);


                            break;
                    }

                    // 在所有解码后的帧都被渲染后，就可以停止播放了
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.e(TAG, "zsr OutputBuffer BUFFER_FLAG_END_OF_STREAM");

                        break;
                    }


                }
                mDecoder.stop();
                mDecoder.release();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class AsyncCode extends Thread {
        // 用于对准视频的时间戳
        long startMs = System.currentTimeMillis();

        @Override
        public void run() {
            super.run();
            try {
                //开始解码，选择要解析的轨道，这里选择使用视频轨
                mMediaExtractor.selectTrack(mVideoTrackIndex);
                mDecoder = MediaCodec.createDecoderByType(mMimeType);


                //根据视频格式，创建赌赢的解码器，API 21以上，通过异步模式进行解码
                mDecoder.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                        //从解码器中，拿到输入的 buffer，让用户填充数据
                        /*ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
                        boolean isDone = false;

                        //从视频中读取数据，填充到 buffer 中
                        int size = mMediaExtractor.readSampleData(inputBuffer, 0);
                        if (size >= 0) {
                            //如果读取到数据了，把buffer给回到解码器
                            mDecoder.queueInputBuffer(
                                    index,
                                    0,
                                    size,
                                    mMediaExtractor.getSampleTime(),
                                    mMediaExtractor.getSampleFlags());
                            mMediaExtractor.advance();
                        }else{
                            //如果下一帧失败，则把buffer扔回去，带上 end of stream 标记，
                            //告之解码器，视频数据已经解析完
                            mDecoder.queueInputBuffer(
                                    index,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            );
                        }*/

                        ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
                        if (inputBuffer != null) {
                            int size = mMediaExtractor.readSampleData(inputBuffer, 0);
                            if (size > 0) {
                                mDecoder.queueInputBuffer(
                                        index,
                                        0,
                                        size,
                                        mMediaExtractor.getSampleTime(),
                                        mMediaExtractor.getSampleFlags());
                            }
                            mMediaExtractor.advance();
                        }

                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                      /*  if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            codec.releaseOutputBuffer(index, false);
                            return;
                        }*/
                        /**
                         * 这个地方先暂时认为每一帧的间隔是30ms，正常情况下，需要根据实际的视频帧的时间标记来计算每一帧的时间点。
                         * 因为视频帧的时间点是相对时间，正常第一帧是0，第二帧比如是第5ms。
                         * 基本思路是：取出第一帧视频数据，记住当前时间点，然后读取第二帧视频数据，再用当前时间点减去第一帧时间点，
                         * 看看相对时间是多少，有没有达到第二帧自己带的相对时间点。如果没有，则sleep一段时间，
                         * 然后再去检查。直到大于或等于第二帧自带的时间点之后，进行视频渲染。
                         */
                       // sleepRender(info, startMs);
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //如果视频数据已经读到结尾，则调用MediaExtractor的seekTo，跳转到视频开头，并且重置解码器。
                        codec.releaseOutputBuffer(index, true);
                        boolean reset = ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);


                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        codec.reset();
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        mVideoFormat = format;
                    }
                });
                //设置渲染的颜色格式为Surface。


                //设置解码数据到指定的Surface上。
                mDecoder.configure(mVideoFormat, new Surface(mTextureView.getSurfaceTexture()), null, 0);
                mDecoder.start();


            } catch (IOException e) {
                e.printStackTrace();
            }
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
                Log.d(TAG, "zsr sleepRender: "+timeDifference);
                Thread.sleep(timeDifference);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
        }
    }
}