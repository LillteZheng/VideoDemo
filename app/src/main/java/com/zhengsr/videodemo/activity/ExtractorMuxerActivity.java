package com.zhengsr.videodemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ExtractorMuxerActivity extends AppCompatActivity {
    private static final String TAG = "ExtractorMuxerActivity";
    private int mVideoTrackId;
    private MediaFormat mVideoFormat;
    private int mAudioTrackId;
    private MediaFormat mAudioFormat;
    private MediaExtractor mMediaExtractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extractor_muxer);

        TextView textView = findViewById(R.id.videoInfo);

        StringBuilder sb = new StringBuilder();
        File file = new File(Constants.VIDEO_PATH);
        sb.append("视频名称: " + file.getName()).append("\n");
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(Constants.VIDEO_PATH);
            int count = mMediaExtractor.getTrackCount();
            sb.append("视频轨道数: " + count).append("\n");
            for (int i = 0; i < count; i++) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                //获取 mime 类型
                String mime = format.getString(MediaFormat.KEY_MIME);
                sb.append(mime).append("\n");
                //视频轨
                if (mime.startsWith("video")) {
                    mVideoTrackId = i;
                    mVideoFormat = format;
                } else if (mime.startsWith("audio")) {
                    //音频轨
                    mAudioTrackId = i;
                    mAudioFormat = format;
                }
            }

            int width = mVideoFormat.getInteger(MediaFormat.KEY_WIDTH);
            int height = mVideoFormat.getInteger(MediaFormat.KEY_HEIGHT);

            sb.append("视频宽高: " + width + "x" + height).append("\n");

            long aLong = mVideoFormat.getLong(MediaFormat.KEY_DURATION);

            sb.append("视频播放总时间: " + (aLong / 1000 / 1000 / 60) + "分钟").append("\n");

            int frameRate = mVideoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);

            sb.append("视频帧率: " + frameRate + "fps").append("\n");

            textView.setText(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new MyMuxer(new MuxerListener() {
            @Override
            public void onstart() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sb.append("\n").append("开始生成视频,请稍等~~").append("\n");
                        textView.setText(sb.toString());
                    }
                });
            }

            @Override
            public void onSuccess(String filePath) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sb.append("视频生成完成，路径为："+filePath).append("\n");
                        textView.setText(sb.toString());
                    }
                });
            }

            @Override
            public void onFail(String errorMsg) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sb.append("视频生成失败："+errorMsg).append("\n");
                        textView.setText(sb.toString());
                    }
                });
            }
        }).start();

    }


    class MyMuxer {
        //创建音频的 MediaExtractor
        MyExtractor audioExtractor = new MyExtractor();
        //创建视频的 MediaExtractor
        MyExtractor videoExtractor = new MyExtractor();
        MediaMuxer mediaMuxer;
        private int audioId;
        private int videoId;
        private MediaFormat audioFormat;
        private MediaFormat videoFormat;
        private MuxerListener listener;
        //新的视频名
        String name = "mixvideo.mp4";
        public MyMuxer(MuxerListener listener) {
            this.listener = listener;
            File dir = new File(Constants.PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(Constants.PATH,name);
            //已存在就先删掉
            if (file.exists()) {
                file.delete();
            }

            try {
                //拿到音频的 mediaformat
                audioFormat = audioExtractor.getAudioFormat();
                //拿到音频的 mediaformat
                videoFormat = videoExtractor.getVideoFormat();
                mediaMuxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void start() {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.onstart();
                        //添加音频
                        audioId = mediaMuxer.addTrack(audioFormat);
                        //添加视频
                        videoId = mediaMuxer.addTrack(videoFormat);
                        //开始混合，等待写入
                        mediaMuxer.start();

                        ByteBuffer buffer = ByteBuffer.allocate(500 * 1024);
                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                        //混合视频
                        int videoSize;
                        //读取视频帧的数据，直到结束
                        while ((videoSize = videoExtractor.readBuffer(buffer, true)) > 0) {
                            info.offset = 0;
                            info.size = videoSize;
                            info.presentationTimeUs = videoExtractor.getCurSampleTime();
                            info.flags = videoExtractor.getCurSampleFlags();
                            mediaMuxer.writeSampleData(videoId, buffer, info);
                        }
                        //写完视频，再把音频混合进去
                        int audioSize;
                        //读取音频帧的数据，直到结束
                        while ((audioSize = audioExtractor.readBuffer(buffer, false)) > 0) {
                            info.offset = 0;
                            info.size = audioSize;
                            info.presentationTimeUs = audioExtractor.getCurSampleTime();
                            info.flags = audioExtractor.getCurSampleFlags();
                            mediaMuxer.writeSampleData(audioId, buffer, info);
                        }
                        //释放资源
                        audioExtractor.release();
                        videoExtractor.release();
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        listener.onSuccess(Constants.PATH+File.separator+name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        listener.onFail(e.getMessage());
                    }


                }
            }).start();

        }


    }
    public interface MuxerListener{
        void onstart();
        void onSuccess(String filePath);
        void onFail(String errorMsg);
    }

    class MyExtractor {
        MediaExtractor mediaExtractor;
        int videoTrackId;
        int audioTrackId;
        MediaFormat videoFormat;
        MediaFormat audioFormat;
        long curSampleTime;
        int curSampleFlags;

        public MyExtractor() {
            try {
                mediaExtractor = new MediaExtractor();
                // 设置数据源
                mediaExtractor.setDataSource(Constants.VIDEO_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //拿到所有的轨道
            int count = mediaExtractor.getTrackCount();
            for (int i = 0; i < count; i++) {
                //根据下标拿到 MediaFormat
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                //拿到 mime 类型
                String mime = format.getString(MediaFormat.KEY_MIME);
                //拿到视频轨
                if (mime.startsWith("video")) {
                    videoTrackId = i;
                    videoFormat = format;
                } else if (mime.startsWith("audio")) {
                    //拿到音频轨
                    audioTrackId = i;
                    audioFormat = format;
                }

            }
        }

        /**
         * 读取一帧的数据
         * @param buffer
         * @return
         */
        int readBuffer(ByteBuffer buffer, boolean video) {
            //先清空数据
            buffer.clear();
            //选择要解析的轨道
            mediaExtractor.selectTrack(video ? videoTrackId : audioTrackId);
            //读取当前帧的数据
            int buffercount = mediaExtractor.readSampleData(buffer, 0);
            if (buffercount < 0) {
                return -1;
            }
            //记录当前时间戳
            curSampleTime = mediaExtractor.getSampleTime();
            //记录当前帧的标志位
            curSampleFlags = mediaExtractor.getSampleFlags();
            //进入下一帧
            mediaExtractor.advance();
            return buffercount;
        }

        /**
         * 获取音频 MediaFormat
         * @return
         */
        public MediaFormat getAudioFormat() {
            return audioFormat;
        }

        /**
         * 获取视频 MediaFormat
         * @return
         */
        public MediaFormat getVideoFormat() {
            return videoFormat;
        }

        /**
         * 获取当前帧的标志位
         * @return
         */
        public int getCurSampleFlags() {
            return curSampleFlags;
        }
        /**
         * 获取当前帧的时间戳
         * @return
         */
        public long getCurSampleTime() {
            return curSampleTime;
        }

        /**
         * 释放资源
         */
        public void release() {
            mediaExtractor.release();
        }
    }


}