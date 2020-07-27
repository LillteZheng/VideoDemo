package com.zhengsr.videodemo.media.codec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.FloatProperty;

import com.zhengsr.videodemo.Constants;
import com.zhengsr.videodemo.media.MyExtractor;

import java.io.IOException;

public abstract class BaseCodec {
    protected final static int VIDEO = 1;
    protected final static int AUDIO = 2;
    protected MediaFormat mediaFormat;
    protected MediaCodec mediaCodec;
    protected MyExtractor extractor;


    public BaseCodec() {
        try {
            //获取 MediaExtractor
            extractor = new MyExtractor(Constants.VIDEO_PATH);
            //判断是音频还是视频
            int type = decodeType();
            //拿到音频或视频的 MediaFormat
            mediaFormat = (type == VIDEO ? extractor.getVideoFormat() : extractor.getAudioFormat());
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            //选择要解析的轨道
            extractor.selectTrack(type == VIDEO ? extractor.getVideoTrackId() : extractor.getAudioTrackId());
            //创建 MediaCodec
            mediaCodec = MediaCodec.createDecoderByType(mime);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected abstract int decodeType();
}
