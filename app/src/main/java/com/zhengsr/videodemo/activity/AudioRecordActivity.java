package com.zhengsr.videodemo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.utils.CloseUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class AudioRecordActivity extends AppCompatActivity {
    private static final String TAG = "AudioRecordActivity";
    private static final int AUDIO_RATE = 44100;
    private static final String PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/VideoDemo";
    private AudioThread mAudioThread;
    private AudioTrackThread mAudioTrackThread;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);


        Button button = findViewById(R.id.record);

        button.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        //开始录制
                        startRecord();
                        //   WindEar.getInstance().startRecord(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mAudioThread != null) {
                            mAudioThread.done();
                        }
                        break;
                    default:
                        break;

                }
                return false;
            }
        });
    }


    /**
     * 开始录制
     */
    private void startRecord() {
        //如果存在，先停止线程
        if (mAudioThread != null) {
            mAudioThread.done();
            mAudioThread = null;
        }
        //开启线程录制
        mAudioThread = new AudioThread();
        mAudioThread.start();
    }

    public void playwav(View view) {

        File file = new File(PATH, "test.wav");
        if (file.exists()) {

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            Uri uri;
            //Android 7.0 以上，需要使用 FileProvider
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(this, "com.zhengsr.videodemo.fileprovider", file);
            } else {
                uri = Uri.fromFile(file.getAbsoluteFile());
            }
            intent.setDataAndType(uri, "audio");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } else {
            Toast.makeText(this, "请先录制", Toast.LENGTH_SHORT).show();
        }
    }

    public void playpcm(View view) {
        if (mAudioTrackThread != null) {
            mAudioTrackThread.down();
            mAudioTrackThread = null;
        }
        //播放pcm文件
        mAudioTrackThread = new AudioTrackThread();
        mAudioTrackThread.start();

        /// getResources().openRawResource(R.raw.ding);

    }

    public void playpcm2(View view) {
        try {
            File file = new File(PATH, "test.pcm");
            InputStream is = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            //创建一个数组
            byte[] buffer = new byte[1024];
            while ((len = is.read(buffer)) > 0) {
                //把数据存到ByteArrayOutputStream中
                baos.write(buffer, 0, len);
            }
            //拿到音频数据
            byte[] bytes = baos.toByteArray();

            //双声道
            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
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
                    .setSampleRate(AUDIO_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelConfig)
                    .build();
            //注意 bufferSizeInBytes 使用音频的大小
            AudioTrack audioTrack = new AudioTrack(
                    attributes,
                    format,
                    bytes.length,
                    AudioTrack.MODE_STATIC, //设置为静态模式
                    AudioManager.AUDIO_SESSION_ID_GENERATE //音频识别id
            );
            //一次性写入
            audioTrack.write(bytes, 0, bytes.length);
            //开始播放
            audioTrack.play();
          
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 音频录制线程
     */
    class AudioThread extends Thread {
        private AudioRecord record;
        private int minBufferSize;
        private boolean isDone = false;

        public AudioThread() {
            /**
             * 获取最小 buffer 大小
             * 采样率为 44100，双声道，采样位数为 16bit
             */
            minBufferSize = AudioRecord.getMinBufferSize(AUDIO_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            //使用 AudioRecord 去录音
            record = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    AUDIO_RATE,
                    AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize
            );
        }

        @Override
        public void run() {
            super.run();
            FileOutputStream fos = null;
            FileOutputStream wavFos = null;
            RandomAccessFile wavRaf = null;
            try {
                //没有先创建文件夹
                File dir = new File(PATH);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                //创建 pcm 文件
                File pcmFile = getFile(PATH, "test.pcm");
                //创建 wav 文件
                File wavFile = getFile(PATH, "test.wav");
                fos = new FileOutputStream(pcmFile);
                wavFos = new FileOutputStream(wavFile);

                //先写头部，刚才是，我们并不知道 pcm 文件的大小
                byte[] headers = generateWavFileHeader(0, AUDIO_RATE, record.getChannelCount());
                wavFos.write(headers, 0, headers.length);

                //开始录制
                record.startRecording();
                byte[] buffer = new byte[minBufferSize];
                while (!isDone) {
                    //读取数据
                    int read = record.read(buffer, 0, buffer.length);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        //写 pcm 数据
                        fos.write(buffer, 0, read);
                        //写 wav 格式数据
                        wavFos.write(buffer, 0, read);
                    }

                }
                //录制结束
                record.stop();
                record.release();

                fos.flush();
                wavFos.flush();

                //修改头部的 pcm文件 大小
                wavRaf = new RandomAccessFile(wavFile, "rw");
                byte[] header = generateWavFileHeader(pcmFile.length(), AUDIO_RATE, record.getChannelCount());
                wavRaf.seek(0);
                wavRaf.write(header);

            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, " run: " + e.getMessage());
            } finally {
                CloseUtils.close(fos, wavFos,wavRaf);
            }
        }

        public void done() {
            interrupt();
            isDone = true;
        }
    }



    class AudioTrackThread extends Thread {
        AudioTrack audioTrack;
        private final int bufferSize;
        private boolean isDone;


        public AudioTrackThread() {

            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;

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
             * 设置音频哥特式
             * 1. 设置采样率
             * 2. 设置采样位数
             * 3. 设置声道
             */
            AudioFormat format = new AudioFormat.Builder()
                    .setSampleRate(AUDIO_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(channelConfig)
                    .build();
            //拿到一帧的最小buffer大小
            bufferSize = AudioTrack.getMinBufferSize(AUDIO_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            audioTrack = new AudioTrack(
                    attributes,
                    format,
                    bufferSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
            );

            audioTrack.play();

        }

        @Override
        public void run() {
            super.run();
            File file = new File(PATH, "test.pcm");
            if (file.exists()) {
                FileInputStream fis = null;
                try {

                    fis = new FileInputStream(file);
                    byte[] buffer = new byte[bufferSize];
                    int len;
                    while (!isDone && (len = fis.read(buffer)) > 0) {
                        audioTrack.write(buffer, 0, len);
                    }

                    audioTrack.stop();
                    audioTrack.release();

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, " run: " + e);
                } finally {
                    CloseUtils.close(fis);
                }

            }

        }

        void down() {
            isDone = true;
        }
    }

    private File getFile(String path, String name) {
        File file = new File(path, name);
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 任何一种文件在头部添加相应的头文件才能够确定的表示这种文件的格式，
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
     * FMT Chunk，Fact chunk,Data chunk,其中Fact chunk是可以选择的
     *
     * @param pcmAudioByteCount 不包括header的音频数据总长度
     * @param longSampleRate    采样率,也就是录制时使用的频率
     * @param channels          audioRecord的频道数量
     */
    private byte[] generateWavFileHeader(long pcmAudioByteCount, long longSampleRate, int channels) {
        long totalDataLen = pcmAudioByteCount + 36; // 不包含前8个字节的WAV文件总长度
        long byteRate = longSampleRate * 2 * channels;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (2 * channels);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (pcmAudioByteCount & 0xff);
        header[41] = (byte) ((pcmAudioByteCount >> 8) & 0xff);
        header[42] = (byte) ((pcmAudioByteCount >> 16) & 0xff);
        header[43] = (byte) ((pcmAudioByteCount >> 24) & 0xff);
        return header;
    }
}