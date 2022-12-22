package com.zhengsr.videodemo.activity.mediaproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcelable;

import androidx.core.app.NotificationCompat;

import com.zhengsr.videodemo.activity.MediaProjectionActivity;


/**
 * 投屏后台服务,API28 之后，投屏需要有一个前台服务
 * @author zhengshaorui
 * @date 2022/03/29
 */

public class MediaProjectService extends Service {
    private static final String TAG = "MediaProjectService";
    private static final String NOTIFICATION_CHANNEL_ID="com.zhengsr.videodemo.activity.mediaproject.MediaService";
    private static final String NOTIFICATION_CHANNEL_NAME="com.zhengsr.videodemo.activity.mediaproject.channel_name";
    private static final String NOTIFICATION_CHANNEL_DESC="com.zhengsr.videodemo.activity.mediaproject.channel_desc";
    private static final int NOTIFICAIONT_ID = 234235;
    private static NotificationManager notificationManager;
    private MediaProjection mMediaProjection;

    public MediaProjectService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startNotification();
    }


    public void startNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Call Start foreground with notification
            Intent notificationIntent = new Intent(this, MediaProjectService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Starting Service")
                    .setContentText("Starting monitoring service")
                    .setContentIntent(pendingIntent);
            Notification notification = notificationBuilder.build();
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            //必须使用此方法显示通知，不能使用notificationManager.notify，否则还是会报上面的错误
            startForeground(NOTIFICAIONT_ID, notification);
        }



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        return super.onStartCommand(intent, flags, startId);
    }

    private  void stopMirror(){
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.cancel(NOTIFICAIONT_ID);
            notificationManager = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        stopMirror();
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopMirror();
        super.onTaskRemoved(rootIntent);
    }
    private MyBinder myBinder;
    @Override
    public IBinder onBind(Intent intent) {
        myBinder = new MyBinder();
        return myBinder;
    }

    public static void release() {

    }
    public  class MyBinder extends Binder implements IProjection{

        @Override
        public void startNotification() {
            MediaProjectService.this.startNotification();
        }

        @Override
        public void registerCallback(int requestCode,int resultCode, Intent data, ICallback callback) {
            if (data != null) {
                MediaProjectionManager projectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                mMediaProjection = projectionManager.getMediaProjection(resultCode, (Intent) data);
                if (callback != null) {
                    callback.onResult(requestCode,mMediaProjection);
                }
            }
        }


    }

    public interface IProjection{
        void startNotification();
        void registerCallback(int requestCode,int resultCode,Intent data,ICallback callback);
        public interface ICallback{
            void onResult(int requestCode,MediaProjection mediaProjection);
        }
    }
}
