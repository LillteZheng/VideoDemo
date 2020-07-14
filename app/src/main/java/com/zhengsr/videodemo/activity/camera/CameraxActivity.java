package com.zhengsr.videodemo.activity.camera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraxActivity extends AppCompatActivity {
    private static final String TAG = "CameraxActivity";
    private PreviewView mViewFinder;
    private ImageCapture mImageCapture;
    private ExecutorService mExecutorService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camerax);
        mExecutorService = Executors.newSingleThreadExecutor();
        //PreviewView，这是一种可以剪裁、缩放和旋转以确保正确显示的 View
        mViewFinder = findViewById(R.id.viewFinder);
        startCamera();
    }

    /**
     * 开启摄像头
     */
    private void startCamera() {
        //返回当前可以绑定生命周期的 ProcessCameraProvider
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @SuppressLint("RestrictedApi")
            @Override
            public void run() {
                try {
                    //将相机的生命周期和activity的生命周期绑定，camerax 会自己释放，不用担心了
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    //预览的 capture，它里面支持角度换算
                    Preview preview = new Preview.Builder().build();

                    //创建图片的 capture
                    mImageCapture = new ImageCapture.Builder()
                            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                            .build();


                    //选择后置摄像头
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                    //预览之前先解绑
                    cameraProvider.unbindAll();

                    //将数据绑定到相机的生命周期中
                    Camera camera = cameraProvider.bindToLifecycle(CameraxActivity.this, cameraSelector, preview, mImageCapture);
                    //将previewview 的 surface 给相机预览
                    preview.setSurfaceProvider(mViewFinder.createSurfaceProvider(camera.getCameraInfo()));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },ContextCompat.getMainExecutor(this));
    }

    public void takePhoto(View view) {
        if (mImageCapture != null) {
            File dir = new File(Constants.PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            //创建文件
            File file = new File(Constants.PATH,"testx.jpg");
            if (file.exists()) {
                file.delete();
            }
            //创建包文件的数据，比如创建文件
            ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

            //开始拍照
            mImageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                //    Uri savedUri = outputFileResults.getSavedUri();
                    Toast.makeText(CameraxActivity.this, "保存成功: ", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Toast.makeText(CameraxActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}