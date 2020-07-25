package com.zhengsr.videodemo.activity.codec;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
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

import com.zhengsr.videodemo.AudioDecoderThread;
import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.activity.codec.AacCodecActivity;
import com.zhengsr.videodemo.media.MyExtractor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaCodecActivity extends AppCompatActivity {
    private static final String TAG = "MediaCodecActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);


    }




    public void decoder(View view) {
        startActivity(new Intent(this,DecodeMediaActivity.class));
    }


    public void encodeAcc(View view) {
        startActivity(new Intent(this, AacCodecActivity.class));
    }
}