package com.zxt.dlna.dmc;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.model.PositionInfo;

import com.zxt.dlna.service.MediaService;
import com.zxt.dlna.util.Action;
import com.zxt.dlna.util.Utils;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

public class GetPositionInfoCallback extends GetPositionInfo {
	private String TAG = "GetPositionInfoCallback";

	private Handler handler;

	private static boolean replay = false;

	private int counter = 0;

	private MediaService mediaService;

 	public GetPositionInfoCallback(Service paramService, Handler paramHandler,
			MediaService mediaService,int counter) {
		super(paramService);
		this.handler = paramHandler;
		this.counter = counter;
		this.mediaService = mediaService;
	}

	public void failure(ActionInvocation paramActionInvocation,
			UpnpResponse paramUpnpResponse, String paramString) {
		Log.e(this.TAG, "failed");
	}

	public void received(ActionInvocation paramActionInvocation,
			PositionInfo paramPositionInfo) {
		Bundle localBundle = new Bundle();
		localBundle.putString("TrackDuration",paramPositionInfo.getTrackDuration());
		localBundle.putString("RelTime", paramPositionInfo.getRelTime());
		Intent localIntent = new Intent(Action.PLAY_UPDATE);

		Log.e("Current Seek Position:", "" + paramPositionInfo.getRelTime());
		int trachTime = Utils.getRealTime(paramPositionInfo.getTrackDuration());
		int reltime = Utils.getRealTime(paramPositionInfo.getRelTime());
		int restartTiem = Utils.getRealTime(paramPositionInfo.getTrackDuration()) - 10;
		if(trachTime!= 0 && ( reltime>=restartTiem)) {
			localBundle.putString("playControl","RepeatOne");
		}
		if(reltime<restartTiem)
		{
			localBundle.putString("playControl","none");
		}
		localIntent.putExtras(localBundle);
		mediaService.sendBroadcast(localIntent);
	}

	public void success(ActionInvocation paramActionInvocation) {
		super.success(paramActionInvocation);
	}

}
