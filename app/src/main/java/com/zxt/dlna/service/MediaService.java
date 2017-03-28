package com.zxt.dlna.service;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.zxt.dlna.R;
import com.zxt.dlna.activity.ContentActivity;
import com.zxt.dlna.activity.ControlActivity;
import com.zxt.dlna.activity.IndexActivity;
import com.zxt.dlna.activity.SettingActivity;
import com.zxt.dlna.application.BaseApplication;
import com.zxt.dlna.application.ConfigData;
import com.zxt.dlna.dmc.DMCControl;
import com.zxt.dlna.dmp.ContentItem;
import com.zxt.dlna.dmp.DeviceItem;
import com.zxt.dlna.util.Action;
import com.zxt.dlna.util.CommonUtil;
import com.zxt.dlna.util.Utils;

import org.fourthline.cling.android.AndroidUpnpService;

import java.util.ArrayList;

import static com.zxt.dlna.application.BaseApplication.mContext;

/**
 * Created by Saudamini on 12/27/2016.
 */

public class MediaService extends Service {

    private static final String TAG = "Media Service:";

    /* variables, constants and declaration related to the SLIDESHOW/IMAGES display */
    protected static final int MSG_SLIDE_START = 1000;

    protected static final int MSG_SLIDE_SHOW_STOP = 2000;

    private String mPlayUri = null;

    private String currentContentFormatMimeType = "";

    private DeviceItem dmrDeviceItem = null;

    private boolean isLocalDmr = true;

    private String metaData = "";

    private DMCControl dmcControl = null;

    private AndroidUpnpService upnpService = null;

    private int mCurrentPosition;

    private static ArrayList<ContentItem> mListPhotos = new ArrayList<ContentItem>();

    private boolean isSlidePlaying = false;


    /*variables, constants and declarations related to the VIDEO/AUDIO display*/
    public static boolean isVideoPlay = false;

    private boolean isUpdatePlaySeek = true;

    public String name;

    private String path;

    private boolean replay = false;

    /*common variables, constants and declarations*/
    private Activity mParentActivity;
        
    private IBinder myBinder = new MyLocalBinder();

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SLIDE_START: {
                    nextImage();
                    int time = SettingActivity.getSlideTime(mParentActivity);
                    if (time < 5) {time = 5;}
                    mHandler.sendEmptyMessageDelayed(MSG_SLIDE_START,time * 1000);
                    break;
                }
                case MSG_SLIDE_SHOW_STOP:{
                    dmcControl.stop(true);
                    dmcControl = null;
                    mHandler.removeCallbacksAndMessages(null);
                }
                break;
                default:
                    break;
            }
        };
    };

    private BroadcastReceiver updatePlayTime = new BroadcastReceiver() {
        public void onReceive(Context paramContext, Intent paramIntent) {
            try {

                if (paramIntent.getAction().equals(Action.PLAY_UPDATE)) {
                    if (isUpdatePlaySeek) {
                        Bundle localBundle = paramIntent.getExtras();
//                        String str1 = localBundle.getString("TrackDuration");
//                        String str2 = localBundle.getString("RelTime");
                        String playControl = localBundle.getString("playControl");
                        if(playControl.contains("RepeatOne") && (replay == false)){
                            replay = true;
                            dmcControl.rePlayControl();
                        }
                        if(playControl.contains("none")){
                            replay = false;
                        }
                    }
                }
                if (paramIntent.getAction().equals(Action.PLAY_ERR_VIDEO)
                        || paramIntent.getAction().equals(Action.PLAY_ERR_AUDIO)) {
                    Toast.makeText(MediaService.this, R.string.media_play_err,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (RuntimeException e){
                e.printStackTrace();
            }
        }

    };

    @Override
    public void onCreate(){
        super.onCreate();
        this.registerReceiver(this.myConnectionReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }
    //start when call from the startsrvice()
    @Override
    public int onStartCommand(Intent intent, int flags, int startid){
        final Intent localIntent = intent;
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Start Foreground Intent ");
            sendNotification("Media Service Started","");
        }
        else if (intent.getAction().equals(Constants.ACTION.STARTSLIDESHOW_ACTION)) {
//           if(mParentActivity != null) {
            initData(localIntent);
            showImage(mPlayUri);
            startSlideShow();
            sendNotification("Starting Slideshow...","Campaign SlideShow");
//           }
        }
        else if(intent.getAction().equals(Constants.ACTION.STARTVIDEO_ACTION)){
            IntentFilter localIntentFilter = new IntentFilter();
            localIntentFilter.addAction(Action.PLAY_UPDATE);
            localIntentFilter.addAction("com.video.play.error");
            localIntentFilter.addAction("com.connection.failed");
            localIntentFilter.addAction("com.transport.info");
            localIntentFilter.addAction(Action.PLAY_ERR_VIDEO);
            localIntentFilter.addAction(Action.PLAY_ERR_AUDIO);
            this.registerReceiver(updatePlayTime, localIntentFilter);
            initVideoData(intent);
            sendNotification("Starting CampaignVideo...","Campaign Video");
        }
        return START_STICKY;
    }
    //client is binding to the service with bindsevice()
    @Override
    public IBinder onBind(Intent intent){
        Log.i(TAG,"TonBind called...");
        return myBinder;
    }
    //class used for the client Binder
    public class MyLocalBinder extends Binder {
        public MediaService getService(){
            /*return the instance of the MediaService
            so that client can access the public methods*/
            return  MediaService.this;
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        dmcControl.stop(true);
        DMCControl.isExit = true;
        BaseApplication.upnpService = null;
        upnpService = null;
        this.unregisterReceiver(myConnectionReceiver);
        this.unregisterReceiver(updatePlayTime);
    }
    //public void setParentActivity(Activity activity){mParentActivity = activity;}
    //notifiy the usser about the service
    public void sendNotification(String msg, String content){
        Intent notificationIntent = new Intent(this, ContentActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_launcher);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("DLNA-SnappBizz")
                .setTicker(msg)
                .setContentText("SnapBizz Campaign")
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                notification);
    }


    /*functions related to images display*/
    private void initData(Intent intent) {

        mPlayUri = intent.getStringExtra("playURI");

        mCurrentPosition = ConfigData.photoPosition;
        mListPhotos = ConfigData.listPhotos;

        dmrDeviceItem = BaseApplication.dmrDeviceItem;
        upnpService = BaseApplication.upnpService;

        isLocalDmr = BaseApplication.isLocalDmr;

        if (!isLocalDmr) {
            if(dmcControl!= null){
                dmcControl.stop(true);
                dmcControl = null;
            }
            currentContentFormatMimeType = intent
                    .getStringExtra("currentContentFormatMimeType");
            metaData = intent.getStringExtra("metaData");
            dmcControl = new DMCControl(this, 1, dmrDeviceItem, upnpService,
                    mPlayUri, metaData);
        }
    }
    public void startSlideShow(){
        if(dmcControl != null){
            mHandler.sendEmptyMessageDelayed(MSG_SLIDE_START, SettingActivity.getSlideTime(mParentActivity) * 1000);
            isSlidePlaying = true;
        }
    }
    private void nextImage() {
        if (mCurrentPosition >= mListPhotos.size() - 1) {
            mCurrentPosition = 0;

        } else {
            mCurrentPosition = mCurrentPosition + 1;
        }
        String uri = mListPhotos.get(mCurrentPosition).getItem().getFirstResource().getValue();
        if (!TextUtils.isEmpty(uri)) {
            mPlayUri = uri;
            showImage(mPlayUri);
        }
    }
    private boolean prevImage() {
        boolean isFirst;
        if (mCurrentPosition == 0) {
            isFirst = true;
            mCurrentPosition = mListPhotos.size() - 1;

        } else {
            isFirst = false;
            mCurrentPosition = mCurrentPosition - 1;
        }
        String uri = mListPhotos.get(mCurrentPosition).getItem().getFirstResource().getValue();
        if (!TextUtils.isEmpty(uri)) {
            mPlayUri = uri;
            showImage(mPlayUri);
        }
        return isFirst;
    }
    private void showImage(String url) {
        if (!isLocalDmr) {
            try {
                dmcControl.setCurrentPlayPath(mPlayUri,metaData);
            } catch (Exception e) {
                e.printStackTrace();
            }
            dmcControl.getProtocolInfos(currentContentFormatMimeType);
        }
    }
    public void setParentActivity(Activity context){
        this.mParentActivity = context;
    }
    public void stopSlideShow(){
        if(isSlidePlaying) {
            dmcControl.stop(true);
            dmcControl = null;
            mHandler.removeCallbacksAndMessages(null);
            isSlidePlaying = false;
        }
    }

    private void initVideoData(Intent localIntent) {
        if (null == localIntent) {
            Toast.makeText(this, getString(R.string.not_select_dev),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Bundle localBundle = localIntent.getExtras();
        path = localBundle.getString("playURI");
        name = localBundle.getString("name");
        currentContentFormatMimeType = localIntent
                .getStringExtra("currentContentFormatMimeType");
        metaData = localBundle.getString("metaData");
        if (dmcControl != null) {
            if (localBundle.getBoolean("restart", true)) {
                dmcControl.stop(true);
                dmcControl = null;
            }
        }
        if (null != path && null != currentContentFormatMimeType
                && null != metaData) {

            isVideoPlay = true;
            // TODO get
            dmrDeviceItem = BaseApplication.dmrDeviceItem;
            upnpService = BaseApplication.upnpService;
            dmcControl = new DMCControl(this, 3, dmrDeviceItem,
                    this.upnpService, this.path, this.metaData);
            dmcControl.getProtocolInfos(currentContentFormatMimeType);
        } else {
            Toast.makeText(this, getString(R.string.get_data_err),
                    Toast.LENGTH_SHORT).show();
        }
    }
    public void stopVideo(){
        if (dmcControl != null) {
            dmcControl.stop(true);
            dmcControl = null;
            if (updatePlayTime != null) {
                unregisterReceiver(updatePlayTime);
            }
        }
    }

    private BroadcastReceiver myConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (CommonUtil.checkNetState(MediaService.this) == 2) {
                if (null != dmcControl && null != path && (!currentContentFormatMimeType.contains("image"))
                        && null != metaData) {
                    isVideoPlay = true;
                    // TODO get
                    dmrDeviceItem = BaseApplication.dmrDeviceItem;
                    upnpService = BaseApplication.upnpService;
                    dmcControl = new DMCControl(MediaService.this, 3, dmrDeviceItem,
                            upnpService, path, metaData);
                    dmcControl.getProtocolInfos(currentContentFormatMimeType);
                    sendNotification("Starting CampaignVideo...","Campaign Video");
                }
            }
        }

    };
}

