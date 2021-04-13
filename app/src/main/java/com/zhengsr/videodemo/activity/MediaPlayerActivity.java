package com.zhengsr.videodemo.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.utils.MediaPlayerHelper;

import java.io.IOException;

public class MediaPlayerActivity extends AppCompatActivity {
    private static final String TAG = "MediaPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);


        SurfaceView surfaceView = findViewById(R.id.surface);


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                MediaPlayerHelper.prepare(Constants.VIDEO_PATH, holder, new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Toast.makeText(MediaPlayerActivity.this, "准备完毕，请点击播放!", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayerHelper.play();
            }
        });
        findViewById(R.id.pause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaPlayerHelper.pause();
            }
        });
    }

    @Override
    protected void onDestroy() {
        MediaPlayerHelper.release();
        super.onDestroy();

    }
}