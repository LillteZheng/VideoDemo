package com.zhengsr.videodemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.zhengsr.videodemo.activity.MediaCodecActivity;
import com.zhengsr.videodemo.activity.MediaPlayerActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void meidaplayer(View view) {
        startActivity(new Intent(this, MediaPlayerActivity.class));
    }

    public void mediaCodec(View view) {
        startActivity(new Intent(this, MediaCodecActivity.class));
    }
}