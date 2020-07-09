package com.zhengsr.videodemo.media;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

/**
 * @author by  zhengshaorui on
 * Describe: 工具类音视频提取器
 */
public class MyExtractor {
    private static final String TAG = "MyExtractor";
    private final MediaExtractor mExtractor;
    private VideoTrack mVideoBean;
    private BaseTrack mAudioBean;
    public MyExtractor(){
        mExtractor = new MediaExtractor();
    }

    public void setDataSource(String path){
        //设置视频路径
        try {
            mExtractor.setDataSource(path);
            //获取轨道数
            int trackCount = mExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                //拿到 MediaFormat
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.d(TAG, "zsr setDataSource: "+mime);
                if ("video/".contains(mime)) {
                    mVideoBean = new VideoTrack();
                    mVideoBean.mime = mime;
                    mVideoBean.format = format;
                    mVideoBean.trackIndex = i;
                    mVideoBean.width = format.getInteger(MediaFormat.KEY_WIDTH);
                    mVideoBean.height = format.getInteger(MediaFormat.KEY_HEIGHT);
                }else if ("audio/".contains(mime)){
                    mAudioBean = new BaseTrack();
                    mAudioBean.mime = mime;
                    mAudioBean.format = format;
                    mAudioBean.trackIndex = i;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void selectTrack(int trackIndex){
        mExtractor.selectTrack(trackIndex);
    }

    public BaseTrack getVideoBean() {
        return mVideoBean;
    }

    public BaseTrack getAudioBean() {
        return mAudioBean;
    }


    private class BaseTrack {
        public String mime;
        public MediaFormat format;
        public int trackIndex = -1;
    }

    private class VideoTrack extends BaseTrack{
        public int width;
        public int height;
    }

}
