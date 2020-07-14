package com.zhengsr.videodemo.activity.camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.zhengsr.videodemo.R;

public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
    }

    public void camera1(View view) {
        startActivity(new Intent(this, Camera1Activity.class));
    }

    public void camera2(View view) {
        startActivity(new Intent(this, Camera2Activity.class));
    }

    public void camerax(View view) {
        startActivity(new Intent(this, CameraxActivity.class));
    }
}