package com.zxt.dlna.dmc;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.Seek;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.zxt.dlna.service.MediaService;

public class SeekCallback extends Seek {

	private String TAG = "seekcallback";
	private MediaService mediaService;
	private Handler mHandler;

	public SeekCallback(MediaService paramMediaService, Service paramService,
						String paramString, Handler paramHandler) {
		super(paramService, paramString);
		mediaService = paramMediaService;
		mHandler = paramHandler;
	}

	public void failure(ActionInvocation paramActionInvocation,
			UpnpResponse paramUpnpResponse, String paramString) {
		Log.e(this.TAG, "failed");
	}

	public void sendBroadcast() {
		Intent localIntent = new Intent("com.continue.display");
		this.mediaService.sendBroadcast(localIntent);
	}

	public void success(ActionInvocation paramActionInvocation) {
		super.success(paramActionInvocation);
		mHandler.sendEmptyMessage(DMCControlMessage.GETPOTITION);
		Log.i(this.TAG, "success");
	}

}
