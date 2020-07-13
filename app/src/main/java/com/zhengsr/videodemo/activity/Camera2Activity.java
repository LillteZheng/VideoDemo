package com.zhengsr.videodemo.activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.R;
import com.zhengsr.videodemo.utils.BitmapUtils;
import com.zhengsr.videodemo.utils.CloseUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Activity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "Camera2Activity";
    private CameraManager mCameraManager;
    private String mFrontCameraId;
    private CameraCharacteristics mFrontCameraCharacteristics;
    private String mBackCameraId;
    private String mCameraId;
    private CameraCharacteristics mBackCameraCharacteristics;
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private TextureView mTextureView;
    private ImageView mImageView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    private Integer mSensorOrientation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera2);


        //初始化相机
        initCamera();

        mTextureView = findViewById(R.id.surface);


        findViewById(R.id.btn1).setOnClickListener(this);
        findViewById(R.id.btn2).setOnClickListener(this);
        mImageView = findViewById(R.id.image);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mTextureView != null && mTextureView.getWidth() != 0) {
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());
        }else{
            mTextureView.setSurfaceTextureListener(new PreviewCallback());
        }
    }


    @Override
    protected void onPause() {
        closeCamera();
        Log.d(TAG, "zsr onPause: ");
        super.onPause();
    }

    /**
     * 初始化相机，和配置相关属性
     */
    private void initCamera() {

        try {
            //获取相机服务 CameraManager
            mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

            //遍历设备支持的相机 ID ，比如前置，后置等
            String[] cameraIdList = mCameraManager.getCameraIdList();
            for (String cameraId : cameraIdList) {
                // 拿到装在所有相机信息的  CameraCharacteristics 类
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                //拿到相机的方向，前置，后置，外置
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (facing != null) {
                    //后置摄像头
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        mBackCameraId = cameraId;
                        mBackCameraCharacteristics = characteristics;
                    }else if (facing == CameraCharacteristics.LENS_FACING_FRONT){
                        //前置摄像头
                        mFrontCameraId = cameraId;
                        mFrontCameraCharacteristics = characteristics;
                    }
                    mCameraId = cameraId;
                }

                //是否支持 Camera2 的高级特性
                Integer level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                /**
                 * 不支持 Camera2 的特性
                 */
                if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY){
                  //  Toast.makeText(this, "您的手机不支持Camera2的高级特效", Toast.LENGTH_SHORT).show();
                 //   break;
                }

            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * 打开摄像头
     * @param width
     * @param height
     */
    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        //判断不同摄像头，拿到 CameraCharacteristics
        CameraCharacteristics characteristics = mCameraId.equals(mBackCameraId) ? mBackCameraCharacteristics : mFrontCameraCharacteristics;
        //拿到配置的map
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        //获取摄像头传感器的方向
        mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        //获取预览尺寸
        Size[] previewSizes = map.getOutputSizes(SurfaceTexture.class);
        //获取最佳尺寸
        Size bestSize = getBestSize(width, height, previewSizes);
        /**
         * 配置预览属性
         * 与 Cmaera1 不同的是，Camera 是把尺寸信息给到 Surface (SurfaceView 或者 ImageReader)，
         * Camera 会根据 Surface 配置的大小，输出对应尺寸的画面;
         * 注意摄像头的 width > height ，而我们使用竖屏，所以宽高要变化一下
         */
        mTextureView.getSurfaceTexture().setDefaultBufferSize(bestSize.getHeight(),bestSize.getWidth());

        /**
         * 设置图片尺寸，这里图片的话，选择最大的分辨率即可
         */
        Size[] sizes = map.getOutputSizes(ImageFormat.JPEG);
        Size largest = Collections.max(
                Arrays.asList(sizes),
                new CompareSizesByArea());
        //设置imagereader，配置大小，且最大Image为 1，因为是 JPEG
        mImageReader = ImageReader.newInstance(largest.getWidth(),largest.getHeight(),
                ImageFormat.JPEG,1);

        //拍照监听
        mImageReader.setOnImageAvailableListener(new ImageAvailable(),null);

        try {
            //打开摄像头，监听数据
            mCameraManager.openCamera(mCameraId,new CameraDeviceCallback(),null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn1){
            mCameraId = mCameraId.equals(mBackCameraId) ? mFrontCameraId:mBackCameraId;
            closeCamera();
            openCamera(mTextureView.getWidth(),mTextureView.getHeight());

        }else{
            try {
                //创建一个拍照的 session
                final CaptureRequest.Builder captureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                //设置装在图像数据的 Surface
                captureRequest.addTarget(mImageReader.getSurface());
                //聚焦
                captureRequest.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //自动曝光
                captureRequest.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 获取设备方向
                int rotation = getWindowManager().getDefaultDisplay().getRotation();
                // 根据设备方向计算设置照片的方向
                captureRequest.set(CaptureRequest.JPEG_ORIENTATION
                        , getOrientation(rotation));
                // 先停止预览
                mCameraCaptureSession.stopRepeating();

                mCameraCaptureSession.capture(captureRequest.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        super.onCaptureCompleted(session, request, result);
                        try {
                            //拍完之后，让它继续可以预览
                            CaptureRequest.Builder captureRequest1 = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            captureRequest1.addTarget(new Surface(mTextureView.getSurfaceTexture()));
                            mCameraCaptureSession.setRepeatingRequest(captureRequest1.build(),null,null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                },null);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    class CameraDeviceCallback extends CameraDevice.StateCallback{

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            //此时摄像头已经打开，可以预览了
            createPreviewPipeline(camera);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
        }
    }

    /**
     * 创建 Session
     */
    private void createPreviewPipeline(CameraDevice cameraDevice){
        try {
            //创建作为预览的 CaptureRequst.builder
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Surface surface = new Surface(mTextureView.getSurfaceTexture());
            //添加 surface 容器
            captureBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求,这个必须在创建 Seesion 之前就准备好，传递给底层用于皮遏制 pipeline
            cameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mCameraCaptureSession = session;
                   try {
                       //设置自动聚焦
                       captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                       //设置自动曝光
                       captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                       //创建 CaptureRequest
                       CaptureRequest build = captureBuilder.build();
                        //设置预览时连续捕获图片数据
                       session.setRepeatingRequest(build,null,null);
                   }catch (Exception e){

                   }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(Camera2Activity.this, "配置失败", Toast.LENGTH_SHORT).show();
                }
            },null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class PreviewCallback implements TextureView.SurfaceTextureListener{

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //当有大小时，打开摄像头
            openCamera(width,height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    /**
     * 拍照监听,当有图片数据时，回调该接口
     */
    class ImageAvailable implements ImageReader.OnImageAvailableListener{

        @Override
        public void onImageAvailable(ImageReader reader) {
            new SavePicAsyncTask(reader).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    private Size getBestSize(int shortSize, int longSize, Size[] sizes) {
        Size bestSize = null;
        float uiRatio = (float) longSize / shortSize;
        float minRatio = uiRatio;
        for (Size previewSize : sizes) {
            float cameraRatio = (float) previewSize.getWidth() / previewSize.getHeight();

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


    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * 关闭摄像头
     */
    private void closeCamera(){
        if (mCameraCaptureSession != null) {
            try {
                //停止预览
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession = null;
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        //关闭设备
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    /**
     * 保存图片
     */
    class SavePicAsyncTask extends AsyncTask<Void, Void, Bitmap> {

        byte[] data;
        File file;
        ImageReader imageReader;
        public SavePicAsyncTask(ImageReader imageReader) {
            this.imageReader = imageReader;
            File dir = new File(Constants.PATH);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = "test2.jpg";
            file = new File(dir, name);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {

            FileOutputStream fos = null;
            Image image = null;
            try {
                fos = new FileOutputStream(file);
                //获取捕获的照片数据
                image = imageReader.acquireLatestImage();
                //拿到所有的 Plane 数组
                Image.Plane[] planes = image.getPlanes();
                //由于是 JPEG ，只需要获取下标为 0 的数据即可
                ByteBuffer buffer = planes[0].getBuffer();
                data = new byte[buffer.remaining()];
                //把 bytebuffer 的数据给 byte数组
                buffer.get(data);
                Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                //旋转图片
                if (mCameraId.equals(mFrontCameraId)){
                    bitmap = BitmapUtils.rotate(bitmap,270);
                    bitmap = BitmapUtils.mirror(bitmap);
                }else{
                    bitmap = BitmapUtils.rotate(bitmap,90);
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                fos.flush();
                return bitmap;
            }catch (Exception e){
                Log.d(TAG, "zsr doInBackground: "+e.toString());
            }finally {
                CloseUtils.close(fos);
                //记得关闭 image
                if (image != null) {
                    image.close();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                mImageView.setVisibility(View.VISIBLE);
                mImageView.setImageBitmap(bitmap);
                Toast.makeText(Camera2Activity.this, "保存成功", Toast.LENGTH_SHORT).show();
                mImageView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mImageView.setVisibility(View.GONE);
                    }
                },2000);
            }else{
                Toast.makeText(Camera2Activity.this, "拍照失败", Toast.LENGTH_SHORT).show();
            }

        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }
}