package com.zhengsr.videodemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaFormat;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import com.gibbs.gplayer.GPlayerView;
import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.media.MyExtractor;

public class TestActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        VideoView view =findViewById(R.id.gl_surface_view);
        view.setVideoPath(Constants.VIDEO_PATH);
        view.start();
    }
}