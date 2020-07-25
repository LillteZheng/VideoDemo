package com.zhengsr.videodemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.zhengsr.videodemo.activity.AudioRecordActivity;
import com.zhengsr.videodemo.activity.ExtractorMuxerActivity;
import com.zhengsr.videodemo.activity.MediaPlayerActivity;
import com.zhengsr.videodemo.activity.camera.CameraActivity;
import com.zhengsr.videodemo.activity.codec.MediaCodecActivity;

import java.io.File;

public  class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA
                },1);

        File file = new File(Constants.VIDEO_PATH);
        if (!file.exists()) {
            Toast.makeText(this, "找不到视频", Toast.LENGTH_SHORT).show();
        }
    }


    public void mediaCodec(View view) {
        startActivity(new Intent(this, MediaCodecActivity.class));
    }

    public void audiorecord(View view) {
        startActivity(new Intent(this, AudioRecordActivity.class));
    }


    public void camera(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    public void mediaplay(View view) {
        startActivity(new Intent(this, MediaPlayerActivity.class));
    }

    public void extractor(View view) {
        startActivity(new Intent(this, ExtractorMuxerActivity.class));
    }
}