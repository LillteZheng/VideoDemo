package com.zhengsr.videodemo.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.utils.BitmapUtils;
import com.zhengsr.videodemo.utils.CloseUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Camera1Activity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Camera1Activity";


    /**
     * logic
     */

    private int mFrontCameraId;
    private Camera.CameraInfo mFrontCameraInfo;
    private int mBackCameraId;
    private Camera.CameraInfo mBackCameraInfo;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private int mCameraID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera1);
        initCamera();

        mSurfaceView = findViewById(R.id.surface);
        mSurfaceView.getHolder().addCallback(new PreviewCallback());

        //打开摄像头
        openCamera(mBackCameraId);

        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
    }



    /**
     * 初始化相机
     */
    private void initCamera() {
        //获取相机个数
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            //获取相机信息
            Camera.getCameraInfo(i, info);
            //前置摄像头
            if (Camera.CameraInfo.CAMERA_FACING_FRONT == info.facing) {
                mFrontCameraId = i;
                mFrontCameraInfo = info;
            } else if (Camera.CameraInfo.CAMERA_FACING_BACK == info.facing) {
                mBackCameraId = i;
                mBackCameraInfo = info;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mSurfaceView != null && mSurfaceView.getWidth() != 0){
            openCamera(mCameraID);
            startPreview(mSurfaceView.getWidth(),mSurfaceView.getHeight());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    /**
     * 开始显示
     */
    private void startPreview(int width, int height) {

        initPreviewParams(width, height);
        //设置预览 SurfaceHolder
        Camera camera = mCamera;
        if (camera != null) {
            try {
                camera.setPreviewDisplay(mSurfaceView.getHolder());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //开始显示
        camera.startPreview();
    }

    /**
     * 打开摄像头
     *
     * @param cameraId
     */
    private void openCamera(int cameraId) {
        //根据 cameraId 打开不同摄像头
        mCamera = Camera.open(cameraId);
        mCameraID = cameraId;
        Camera.CameraInfo info = cameraId == mFrontCameraId ? mFrontCameraInfo : mBackCameraInfo;
        adjustCameraOrientation(info);
    }

    /**
     * 矫正相机预览画面
     *
     * @param info
     */
    private void adjustCameraOrientation(Camera.CameraInfo info) {
        //判断当前的横竖屏
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        int degress = 0;
        //获取手机的方向
        switch (rotation) {
            case Surface.ROTATION_0:
                degress = 0;
                break;
            case Surface.ROTATION_90:
                degress = 90;
                break;
            case Surface.ROTATION_180:
                degress = 180;
                break;
            case Surface.ROTATION_270:
                degress = 270;
                break;
        }
        int result = 0;
        //后置摄像头
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = (info.orientation - degress + 360) % 360;
        } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //先镜像
            result = (info.orientation + degress) % 360;
            result = (360 - result) % 360;
        }
        mCamera.setDisplayOrientation(result);

    }

    /**
     * 设置预览参数，需要制定尺寸才行
     * 在相机中，width > height 的，而我们的UI是3:4，所以这里也要做换算
     *
     * @param shortSize
     * @param longSize
     */
    private void initPreviewParams(int shortSize, int longSize) {
        Camera camera = mCamera;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            //获取手机支持的尺寸
            List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
            Camera.Size bestSize = getBestSize(shortSize, longSize, sizes);
            //设置预览大小
            parameters.setPreviewSize(bestSize.width, bestSize.height);
            //设置图片大小，拍照
            parameters.setPictureSize(bestSize.width, bestSize.height);
            //设置格式
            parameters.setPreviewFormat(ImageFormat.NV21);
            //设置聚焦
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            camera.setParameters(parameters);
        }
    }


    /**
     * 获取预览最后尺寸
     *
     * @param shortSize
     * @param longSize
     * @param sizes
     * @return
     */
    private Camera.Size getBestSize(int shortSize, int longSize, List<Camera.Size> sizes) {
        Camera.Size bestSize = null;
        float uiRatio = (float) longSize / shortSize;
        float minRatio = uiRatio;
        for (Camera.Size previewSize : sizes) {
            float cameraRatio = (float) previewSize.width / previewSize.height;

            //如果找不到比例相同的，找一个最近的,防止预览变形
            float offset = Math.abs(cameraRatio - minRatio);
            if (offset < minRatio) {
                minRatio = offset;
                bestSize = previewSize;
            }
            //比例相同
            if (uiRatio == cameraRatio) {
                bestSize = previewSize;
                break;
            }

        }
        return bestSize;
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn1) {
            //关闭摄像头
            closeCamera();

            mCameraID = mCameraID == mFrontCameraId ? mBackCameraId : mFrontCameraId;
            //打开相机
            openCamera(mCameraID);
            //开启预览
            startPreview(mSurfaceView.getWidth(), mSurfaceView.getHeight());

        } else {
            Camera camera = mCamera;
            camera.takePicture(new Camera.ShutterCallback() {
                @Override
                public void onShutter() {

                }
            }, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    new SavePicAsyncTask(data).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            });
        }
    }

    /**
     * 保存图片
     */
    class SavePicAsyncTask extends AsyncTask<Void, Void, File> {

        byte[] data;
        File file;

        public SavePicAsyncTask(byte[] data) {
            this.data = data;
            File dir = new File(Constants.PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = "test.jpg";
            file = new File(dir, name);
        }

        @Override
        protected File doInBackground(Void... voids) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null) {
                return null;
            }
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                //保存之前先调整方向
                Camera.CameraInfo info = mCameraID == mFrontCameraId ? mFrontCameraInfo : mBackCameraInfo;
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    bitmap = BitmapUtils.rotate(bitmap, 90);
                } else {
                    bitmap = BitmapUtils.rotate(bitmap, 270);
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);


            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                CloseUtils.close(fos);
            }
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (file != null) {
                Toast.makeText(Camera1Activity.this, "图片保存成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Camera1Activity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera() {
        //停止预览
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    class PreviewCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            startPreview(width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCamera();
    }
}