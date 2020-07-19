package com.zhengsr.videodemo.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AacTrackActivity extends AppCompatActivity {
    public static String[] MICROPHONE = {Manifest.permission.RECORD_AUDIO};
    public static String[] STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private MyAudioTrack myAudioTrack;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        myAudioTrack = new MyAudioTrack();
        new Thread(new Runnable() {
            @Override
            public void run() {
                myAudioTrack.start();
            }
        }).start();
    }

    private class MyAudioTrack {
        private AudioTrack mPlayer;
        private MediaCodec mDecoder;
        private MediaExtractor mExtractor;

        public void start() {
            try {
                mExtractor = new MediaExtractor();
                mExtractor.setDataSource(Constants.VIDEO_PATH);
                MediaFormat mFormat = null;
                int samplerate = 0;
                int changelConfig = 0;
                int selectTrack = 0;
                String mine = MediaFormat.MIMETYPE_AUDIO_AAC;
                for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                    mFormat = mExtractor.getTrackFormat(i);
                    mine = mFormat.getString(MediaFormat.KEY_MIME);
                    if (mine.startsWith("audio/")) {
                        selectTrack = i;
                        samplerate = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                        changelConfig = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                        break;
                    }
                }
                mExtractor.selectTrack(selectTrack);

                int minBufferSize = AudioTrack.getMinBufferSize(samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT);
                mPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, samplerate, changelConfig, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM);
                mPlayer.play();

                mDecoder = MediaCodec.createDecoderByType(mine);
                mDecoder.configure(mFormat, null, null, 0);
                mDecoder.start();
                decodeAndPlay();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AacTrackActivity.this, "文件不存在",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void decodeAndPlay() {
            boolean isFinish = false;
            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            while (!isFinish) {
                int inputIdex = mDecoder.dequeueInputBuffer(10000);
                if (inputIdex < 0) {
                    isFinish = true;
                }
                ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIdex);
                inputBuffer.clear();
                int samplesize = mExtractor.readSampleData(inputBuffer, 0);
                if (samplesize > 0) {
                    mDecoder.queueInputBuffer(inputIdex, 0, samplesize, 0, 0);
                    mExtractor.advance();
                } else {
                    isFinish = true;
                }
                int outputIndex = mDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);
                ByteBuffer outputBuffer;
                byte[] chunkPCM;
                while (outputIndex >= 0) {
                    outputBuffer = mDecoder.getOutputBuffer(outputIndex);
                    chunkPCM = new byte[decodeBufferInfo.size];
                    outputBuffer.get(chunkPCM);
                    outputBuffer.clear();
                    mPlayer.write(chunkPCM, 0, decodeBufferInfo.size);
                    mDecoder.releaseOutputBuffer(outputIndex, false);
                    outputIndex = mDecoder.dequeueOutputBuffer(decodeBufferInfo, 10000);
                }
            }
            release();
        }

        private void release() {
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
                mDecoder = null;
            }

            if (mExtractor != null) {
                mExtractor.release();
                mExtractor = null;
            }

            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(AacTrackActivity.this, "播放完成", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    private void checkRecordPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, MICROPHONE, 1);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, STORAGE, 1);
            return;
        }
    }
}