package com.zhengsr.videodemo.activity.codec;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.icu.text.IDNA;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.utils.CloseUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AacCodecActivity extends AppCompatActivity {
    private static final String TAG = "AacCodecActivity";
    private AudioRecordThread mRecordThread;
    private TextView mTextView;
    StringBuilder mSb = new StringBuilder();
    private CountDownLatch mDownLatch = new CountDownLatch(1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aac_codec);
        mTextView = findViewById(R.id.info);
        initRecord();
    }

    /**
     * 录音
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initRecord() {

        Button button = findViewById(R.id.record);
        button.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mRecordThread = new AudioRecordThread();
                        mRecordThread.start();
                        mSb.append("开始录制~ ").append("\n");
                        mTextView.setText(mSb.toString());
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mSb.append("录制完成！！！ ").append("\n");
                        mSb.append("开始编码成 AAC 格式...").append("\n");
                        mTextView.setText(mSb.toString());
                        if (mRecordThread != null) {
                            mRecordThread.done();
                        }
                        try {
                            mDownLatch.await(2000, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        new EncodeAacThread().start();
                       /* String pcmPath = Constants.PATH+ "/codecAudio.pcm";
                        String audioPath = Constants.PATH+ "/codec.aac";
                        new Thread(new AudioEncodeRunnable(pcmPath,audioPath)).start();*/
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecordThread != null) {
            mRecordThread.done();
        }
    }

    class EncodeAacThread extends Thread {
        private boolean isDone;
        private MediaCodec encode;

        public EncodeAacThread() {
            try {
                /**
                 * 设置MediaFormat
                 * 1. 编码为 AAC
                 * 2. 采样率为 44.1Khz
                 * 3. 声音通道个数为 2，即双声道或者立体声
                 */
                MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC,
                        44100, 2);
                //设置比特率，表示单位时间内传送比特的数目，96000 和 115200 是比较常用的
                format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
                //设置 AAC 格式，配置 LC 模式，低复杂度规格，编码效率高，比较简单，没有增益控制，目前 MP4 格式就使用这种规格
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 500 * 1024);

                encode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                encode.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            //开始编码
            encode.start();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            FileInputStream fis = null;
            FileOutputStream fos = null;
            try {
                fis = new FileInputStream(Constants.PATH + "/codecAudio.pcm");
                fos = new FileOutputStream(Constants.PATH + "/codec.aac");

                byte[] bytes = new byte[4 * 1024];
                int len;

                while (!isDone) {

                    if (((len = fis.read(bytes)) > 0)) {

                        //单位是 us
                        int inputBufferId = encode.dequeueInputBuffer(-1);
                        if (inputBufferId > 0) {
                            //拿到编码的空buffer
                            ByteBuffer buffer = encode.getInputBuffer(inputBufferId);

                            if (buffer != null) {
                                //写数据到 buffer
                                buffer.put(bytes, 0, len);
                                encode.queueInputBuffer(
                                        inputBufferId,
                                        0,
                                        len,
                                        0,
                                        0
                                );
                            }

                            //编码之后的数据
                            int outputBufferId = encode.dequeueOutputBuffer(info, 10000);
                            while (outputBufferId > 0) {
                                //拿到当前这一帧的大小
                                int outBitSize = info.size;
                                int outPacketSize = outBitSize + 7;//7为ADT头部的大小
                                ByteBuffer outputBuffer = encode.getOutputBuffer(outputBufferId);
                                if (outputBuffer != null) {
                                    //数据开始的偏移量
                                    outputBuffer.position(info.offset);
                                    outputBuffer.limit(info.offset + outBitSize);
                                    byte[] chunkAudio = new byte[outPacketSize];
                                    addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS
                                    //将编码得到的AAC数据 取出到byte[]中
                                    outputBuffer.get(chunkAudio, 7, outBitSize);

                                    //录制aac音频文件，保存在手机内存中
                                    fos.write(chunkAudio, 0, chunkAudio.length);
                                    fos.flush();

                                    outputBuffer.position(info.offset);
                                    encode.releaseOutputBuffer(outputBufferId, false);
                                    outputBufferId = encode.dequeueOutputBuffer(info, 10000);

                                }
                            }


                        }
                    } else {
                        isDone = true;
                    }
                }


                encode.stop();
                encode.release();

                Log.d(TAG, "zsr run: 编码完成");

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                CloseUtils.close(fis, fos);
            }


        }
    }


    /**
     * 写入ADTS头部数据
     *
     * @param packet
     * @param packetLen
     */
    public static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE

        //syncword，比如为1，即0xff
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    class AudioRecordThread extends Thread {
        //44.1khz
        final static int SAMPLE_RATE = 44100;
        private final AudioRecord audioRecord;
        private final int minBufferSize;
        boolean isDone = false;

        public AudioRecordThread() {
            int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
            int encodingPcm16bit = AudioFormat.ENCODING_PCM_16BIT;
            minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, encodingPcm16bit);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    channelConfig,
                    encodingPcm16bit,
                    minBufferSize
            );
            Log.d(TAG, "zsr AudioRecordThread: " + minBufferSize);
        }

        @Override
        public void run() {
            super.run();
            //开始录制
            audioRecord.startRecording();
            FileOutputStream fos = null;

            try {
                //创建 pcm 文件
                File pcmFile = getFile(Constants.PATH, "codecAudio.pcm");
                fos = new FileOutputStream(pcmFile);
                byte[] buffer = new byte[minBufferSize];
                while (!isDone) {
                    //读取数据
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        //写 pcm 数据
                        fos.write(buffer, 0, read);
                    } else {
                        isDone = true;
                    }

                }

                done();
                mDownLatch.countDown();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    CloseUtils.close(fos);
                }
            }


        }

        private void done() {
            isDone = true;
            audioRecord.stop();
            audioRecord.release();
        }
    }

    private File getFile(String path, String name) {

        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

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
}