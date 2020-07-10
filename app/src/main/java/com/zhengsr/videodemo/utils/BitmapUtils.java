package com.zhengsr.videodemo.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * @author by  zhengshaorui on
 * Describe:
 */
public class BitmapUtils {

    /**
     * 镜像
     * @param bitmap
     * @return
     */
    public static Bitmap mirror(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postScale(-1f,1f);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }

    /**
     * 旋转图片
     * @param bitmap
     * @param degress
     * @return
     */
    public static Bitmap rotate(Bitmap bitmap,float degress){
        Matrix matrix = new Matrix();
        matrix.postRotate(degress);
        return Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
    }



}
