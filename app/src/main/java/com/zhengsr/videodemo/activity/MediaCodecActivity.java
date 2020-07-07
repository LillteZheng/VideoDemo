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
import android.widget.LinearLayout;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;

import java.io.BufferedOutputStream;
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
        init();
        initTractor();
    }

    private void initTractor() {
        //通过 MediaExtractor 解析视频
        try {
            mMediaExtractor = new MediaExtractor();
            //设置视频路径
            mMediaExtractor.setDataSource(Constants.VIDEO_PATH);
            int trackCount = mMediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
                String mime = trackFormat.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "zsr onSurfaceTextureAvailable: " + mime);
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
            mTextureView.post(new Runnable() {
                @Override
                public void run() {
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mWidth, mHeight);
                    mTextureView.setLayoutParams(params);
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        mTextureView = findViewById(R.id.surface);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                try {
                    //开始解码，选择视频轨道
                    mMediaExtractor.selectTrack(mVideoTrackIndex);
                    //根据视频格式，创建赌赢的解码器，API 21以上，通过异步模式进行解码
                    mDecoder = MediaCodec.createDecoderByType(mMimeType);
                    mDecoder.setCallback(new MediaCodec.Callback() {
                        @Override
                        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                            //从解码器中，拿到输入的 buffer，让用户填充数据
                            ByteBuffer inputBuffer = mDecoder.getInputBuffer(index);
                            boolean isDone = false;
                            while (!isDone) {
                                //从视频中读取数据
                                int size = mMediaExtractor.readSampleData(inputBuffer, 0);
                                long sampleTime = mMediaExtractor.getSampleTime();
                                if (size >= 0) {
                                    //如果读取到数据了，把buffer给回到解码器
                                    mDecoder.queueInputBuffer(
                                            index,
                                            0,
                                            size,
                                            sampleTime,
                                            mMediaExtractor.getSampleFlags());
                                }
                                isDone = !mMediaExtractor.advance();

                                if (isDone) {
                                    //如果下一帧失败，则把buffer扔回去，带上 end of stream 标记，
                                    //告之解码器，视频数据已经解析完
                                    mDecoder.queueInputBuffer(
                                            index,
                                            0,
                                            0,
                                            0,
                                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    );
                                }
                                if (size >= 0) {
                                    break;
                                }

                            }
                        }

                        @Override
                        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                codec.releaseOutputBuffer(index, false);
                                return;
                            }
                            /**
                             * 这个地方先暂时认为每一帧的间隔是30ms，正常情况下，需要根据实际的视频帧的时间标记来计算每一帧的时间点。
                             * 因为视频帧的时间点是相对时间，正常第一帧是0，第二帧比如是第5ms。
                             * 基本思路是：取出第一帧视频数据，记住当前时间点，然后读取第二帧视频数据，再用当前时间点减去第一帧时间点，看看相对时间是多少，有没有
                             * 达到第二帧自己带的相对时间点。如果没有，则sleep一段时间，然后再去检查。直到大于或等于第二帧自带的时间点之后，进行视频渲染。
                             */
                            try {
                                Thread.sleep(30);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //如果视频数据已经读到结尾，则调用MediaExtractor的seekTo，跳转到视频开头，并且重置解码器。
                            codec.releaseOutputBuffer(index, true);
                            boolean reset = ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0);
                            if (reset) {
                                mMediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                                mDecoder.flush();
                                mDecoder.start();
                            }

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

                } catch (IOException e) {
                    e.printStackTrace();
                }
                //设置渲染的颜色格式为Surface。
                mVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                //设置解码数据到指定的Surface上。
                mDecoder.configure(mVideoFormat, new Surface(mTextureView.getSurfaceTexture()), null, 0);
                mDecoder.start();
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
        }
    }
}