package com.zxt.dlna.activity;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.registry.DefaultRegistryListener;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.DIDLObject.Property;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;
import org.seamless.util.logging.LoggingUtil;

import com.zxt.dlna.application.BaseApplication;
import com.zxt.dlna.dmp.DeviceItem;
import com.zxt.dlna.dmr.ZxtMediaRenderer;
import com.zxt.dlna.dms.ContentNode;
import com.zxt.dlna.dms.ContentTree;
import com.zxt.dlna.dms.MediaServer;
import com.zxt.dlna.util.CommonUtil;
import com.zxt.dlna.util.FixedAndroidHandler;
import com.zxt.dlna.util.ImageUtil;
import com.zxt.dlna.util.PrepareMedia;
import com.zxt.dlna.util.Utils;
import com.zxt.dlna.R;
import static com.zxt.dlna.application.BaseApplication.mContext;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DevicesActivity extends Activity {
	private boolean reconnection;

	private static final Logger log = Logger.getLogger(DevicesActivity.class
			.getName());

	private final static String LOGTAG = "DevicesActivity";

	public static final int DMR_GET_NO = 0;

	public static final int DMR_GET_SUC = 1;

	private static boolean serverPrepared = false;

	private String fileName;

	private ListView mDevLv;

	private ListView mDmrLv;

	private LinearLayout mDiplayMsg;

	private ArrayList<DeviceItem> mDevList = new ArrayList<DeviceItem>();

	private ArrayList<DeviceItem> mDmrList = new ArrayList<DeviceItem>();

	private int mImageContaierId = Integer.valueOf(ContentTree.IMAGE_ID) + 1;

	private long exitTime = 0;

	private DevAdapter mDevAdapter;

	private DevAdapter mDmrDevAdapter;

	private AndroidUpnpService upnpService;

	private DeviceListRegistryListener deviceListRegistryListener;

	private MediaServer mediaServer;

	private static String registerMediaPlayer;

	private static String registerMediaServer;

	private boolean mediaPlayerSelected = false;

	private boolean mediaServerSelected = false;

	private SharedPreferences prefs;

	protected static final int WAIT_FOR_DEVICE = 1;
	protected static final int DONE_DEVICE_ADDING = 2;
	protected static final int INTIMATE_DISCONNECTION_TO_SNAPBILLING = 3;
	protected static final int INTIMATE_CONNECTION_TO_SNAPBILLING = 4;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case DONE_DEVICE_ADDING:
					mDiplayMsg.setVisibility(View.GONE);
					if (mediaPlayerSelected == true && mediaServerSelected == true) {
						//IndexActivity.setContentPageSelected();
					} else {
						Toast.makeText(DevicesActivity.this, "Please seletct the player and the server.", Toast.LENGTH_LONG);
					}
					break;
				case WAIT_FOR_DEVICE:
					mDiplayMsg.setVisibility(View.VISIBLE);
					break;
				case INTIMATE_DISCONNECTION_TO_SNAPBILLING:
					//TODO:function to handle the disconnection from the snapbilling application
					break;
				case INTIMATE_CONNECTION_TO_SNAPBILLING:
					//TODO:function to handle the connection from the snapbilling application
					break;
			}
		}
	};

	private ServiceConnection serviceConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {

			mDevList.clear();
			mDmrList.clear();

			upnpService = (AndroidUpnpService) service;
			BaseApplication.upnpService = upnpService;

			Log.v(LOGTAG, "Connected to UPnP Service");

			if (mediaServer == null
					&& SettingActivity.getDmsOn(DevicesActivity.this)) {
				try {
					mediaServer = new MediaServer(DevicesActivity.this);

					BaseApplication.mediaServer = mediaServer;
					upnpService.getRegistry()
							.addDevice(mediaServer.getDevice());
					DeviceItem localDevItem = new DeviceItem(
							mediaServer.getDevice());

					deviceListRegistryListener.deviceAdded(localDevItem);
					if(!serverPrepared) {
							new Thread(new Runnable() {

								@Override
								public void run() {
									Looper.prepare();
									mHandler.sendEmptyMessage(INTIMATE_CONNECTION_TO_SNAPBILLING);
									mHandler.sendEmptyMessage(WAIT_FOR_DEVICE);
									prepareMediaServer();
									mHandler.sendEmptyMessage(DONE_DEVICE_ADDING);
									Looper.loop();
								}

							}).start();
					}

				} catch (Exception ex) {
					// TODO: handle exception
					log.log(Level.SEVERE, "Creating demo device failed", ex);
					Toast.makeText(DevicesActivity.this,
							R.string.create_demo_failed, Toast.LENGTH_SHORT)
							.show();
					return;
				}
			}

			if (SettingActivity.getRenderOn(DevicesActivity.this)) {
				ZxtMediaRenderer mediaRenderer = new ZxtMediaRenderer(1,
						DevicesActivity.this);
				upnpService.getRegistry().addDevice(mediaRenderer.getDevice());
				deviceListRegistryListener.dmrAdded(new DeviceItem(
						mediaRenderer.getDevice()));
			}

			// xgf
			for (Device device : upnpService.getRegistry().getDevices()) {
				if (device.getType().getNamespace().equals("schemas-upnp-org")
						&& device.getType().getType().equals("MediaServer")) {
					final DeviceItem display = new DeviceItem(device, device
							.getDetails().getFriendlyName(),
							device.getDisplayString(), "(REMOTE) "
							+ device.getType().getDisplayString());
					deviceListRegistryListener.deviceAdded(display);
				}
			}

			// Getting ready for future device advertisements
			upnpService.getRegistry().addListener(deviceListRegistryListener);
			// Refresh device list
			upnpService.getControlPoint().search();

			// select first device by default
			if (null != mDevList && mDevList.size() > 0
					&& null == BaseApplication.deviceItem) {
				for (DeviceItem dev : mDevList) {
					String devFriendlyName = dev.getDevice().getDetails().getFriendlyName();
					if (devFriendlyName.equals(registerMediaServer))
						BaseApplication.deviceItem = dev;
					mediaServerSelected = true;
					mDevAdapter.notifyDataSetChanged();
					//mHandler.sendEmptyMessageDelayed(MSG_GOTO_CONTENT , CONTEXT_SWITCHING_DELAY);
				}
			}

			if (null != mDmrList && mDmrList.size() > 0
					&& null == BaseApplication.dmrDeviceItem) {
				for (DeviceItem dmr : mDmrList) {
					String dmrFriendlyName = dmr.getDevice().getDetails().getFriendlyName();
					if (dmrFriendlyName.equals(registerMediaPlayer)) {
						BaseApplication.isLocalDmr = false;
						BaseApplication.deviceItem = dmr;
						mediaPlayerSelected = true;
						mDevAdapter.notifyDataSetChanged();
//						mHandler.sendEmptyMessageDelayed(MSG_GOTO_CONTENT , CONTEXT_SWITCHING_DELAY);
//						Toast.makeText(mContext, "Devices are Selected. Switching to content page",
//								Toast.LENGTH_SHORT).show();
					}
				}
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			upnpService = null;
		}
	};

	private DeviceItem selectedDisplayDevice = null;

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
    /* remember you would need to actually initialize these variables before putting it in the
    Bundle */
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Fix the logging integration between java.util.logging and Android
		// internal logging
		LoggingUtil.resetRootHandler(new FixedAndroidHandler());
		Logger.getLogger("org.teleal.cling").setLevel(Level.INFO);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		registerMediaPlayer = SettingActivity.getRegisterdMediaPlayer(this);
		registerMediaServer = SettingActivity.getRegisterdMediaServer(this);

		setContentView(R.layout.devices);
		init();

		deviceListRegistryListener = new DeviceListRegistryListener();

		getApplicationContext().bindService(
				new Intent(this, AndroidUpnpServiceImpl.class),
				serviceConnection, Context.BIND_AUTO_CREATE);

		this.registerReceiver(this.myWifiReceiver,
				new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	private void init() {

		mDevLv = (ListView) findViewById(R.id.media_server_list);

		if (null != mDevList && mDevList.size() > 0) {
			BaseApplication.deviceItem = mDevList.get(0);
			mediaServerSelected = true;
		}

		mDevAdapter = new DevAdapter(DevicesActivity.this, 0, mDevList);
		mDevLv.setAdapter(mDevAdapter);
		mDevLv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
									long arg3) {

				if (null != mDevList && mDevList.size() > 0) {
					BaseApplication.deviceItem = mDevList.get(arg2);
					mDevAdapter.notifyDataSetChanged();
					mediaServerSelected = true;
					//mHandler.sendEmptyMessageDelayed(MSG_GOTO_CONTENT , CONTEXT_SWITCHING_DELAY);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString("media_server", BaseApplication.deviceItem.getDevice().
							getDetails().getFriendlyName());
					if (!editor.commit())
						Toast.makeText(mContext, "Error in registering the device", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, "Successfully registered the device", Toast.LENGTH_SHORT).show();
				}

			}
		});

		mDmrLv = (ListView) findViewById(R.id.renderer_list);

//		if (null != mDmrList && mDmrList.size() > 0) {
//			BaseApplication.dmrDeviceItem = mDmrList.get(0);
//		}

		mDmrDevAdapter = new DevAdapter(DevicesActivity.this, 0, mDmrList);
		mDmrLv.setAdapter(mDmrDevAdapter);
		mDmrLv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
									long arg3) {

				if (null != mDmrList && mDmrList.size() > 0) {
					if (null != mDmrList.get(arg2).getDevice()
							&& null != BaseApplication.deviceItem
							&& null != mDmrList.get(arg2).getDevice()
							.getDetails().getModelDetails()
							&& Utils.DMR_NAME.equals(mDmrList.get(arg2)
							.getDevice().getDetails().getModelDetails()
							.getModelName())
							&& Utils.getDevName(
							mDmrList.get(arg2).getDevice().getDetails()
									.getFriendlyName()).equals(
							Utils.getDevName(BaseApplication.deviceItem
									.getDevice().getDetails()
									.getFriendlyName()))) {
						BaseApplication.isLocalDmr = true;
					} else {
						BaseApplication.isLocalDmr = false;
					}
					BaseApplication.dmrDeviceItem = mDmrList.get(arg2);
					mDmrDevAdapter.notifyDataSetChanged();
					mediaPlayerSelected = true;
					//mHandler.sendEmptyMessageDelayed(MSG_GOTO_CONTENT , CONTEXT_SWITCHING_DELAY);
					selectedDisplayDevice = BaseApplication.dmrDeviceItem;
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString("media_player", BaseApplication.dmrDeviceItem.getDevice().
							getDetails().getFriendlyName());
					if (!editor.commit())
						Toast.makeText(mContext, "Error in registering the device", Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext, "Sucessfully registered the device", Toast.LENGTH_SHORT).show();
				}
			}
		});

		mDiplayMsg = (LinearLayout) findViewById(R.id.display_msg);
	}
	@Override
	protected void onResume(){
		super.onResume();
		selectedDisplayDevice = BaseApplication.dmrDeviceItem;

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (upnpService != null) {
			upnpService.getRegistry()
					.removeListener(deviceListRegistryListener);
		}
		getApplicationContext().unbindService(serviceConnection);
		this.unregisterReceiver(myWifiReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, R.string.search_lan).setIcon(
				android.R.drawable.ic_menu_search);
		menu.add(0, 1, 0, R.string.menu_exit).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case 0:
				searchNetwork();
				break;
			case 1: {
				finish();
				System.exit(0);
				break;
			}
		}
		return false;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			if ((System.currentTimeMillis() - exitTime) > 2000) {
				Toast.makeText(getApplicationContext(), R.string.exit,
						Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				finish();
				System.exit(0);
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	protected void searchNetwork() {
		if (upnpService == null)
			return;
		Toast.makeText(this, R.string.searching_lan, Toast.LENGTH_SHORT).show();
		upnpService.getRegistry().removeAllRemoteDevices();
		upnpService.getControlPoint().search();
	}

	public class DeviceListRegistryListener extends DefaultRegistryListener {

		/* Discovery performance optimization for very slow Android devices! */

		@Override
		public void remoteDeviceDiscoveryStarted(Registry registry,
												 RemoteDevice device) {
		}

		@Override
		public void remoteDeviceDiscoveryFailed(Registry registry,
												final RemoteDevice device, final Exception ex) {
		}

		/*
		 * End of optimization, you can remove the whole block if your Android
		 * handset is fast (>= 600 Mhz)
		 */

		@Override
		public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
			Log.e("DeviceListRegistryListener",
					"remoteDeviceAdded:" + device.toString()
							+ device.getType().getType());

			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaServer")) {
				final DeviceItem display = new DeviceItem(device, device
						.getDetails().getFriendlyName(),
						device.getDisplayString(), "(REMOTE) "
						+ device.getType().getDisplayString());
				deviceAdded(display);
			}

			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaRenderer")) {
				final DeviceItem dmrDisplay = new DeviceItem(device, device
						.getDetails().getFriendlyName(),
						device.getDisplayString(), "(REMOTE) "
						+ device.getType().getDisplayString());
				dmrAdded(dmrDisplay);
			}
		}

		@Override
		public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);

			if (device.getType().getNamespace().equals("schemas-upnp-org")
					&& device.getType().getType().equals("MediaRenderer")) {
				final DeviceItem dmrDisplay = new DeviceItem(device, device
						.getDetails().getFriendlyName(),
						device.getDisplayString(), "(REMOTE) "
						+ device.getType().getDisplayString());
				dmrRemoved(dmrDisplay);
			}
		}

		@Override
		public void localDeviceAdded(Registry registry, LocalDevice device) {
			Log.e("DeviceListRegistryListener",
					"localDeviceAdded:" + device.toString()
							+ device.getType().getType());

			final DeviceItem display = new DeviceItem(device, device
					.getDetails().getFriendlyName(), device.getDisplayString(),
					"(REMOTE) " + device.getType().getDisplayString());
			deviceAdded(display);
		}

		@Override
		public void localDeviceRemoved(Registry registry, LocalDevice device) {
			Log.e("DeviceListRegistryListener",
					"localDeviceRemoved:" + device.toString()
							+ device.getType().getType());

			final DeviceItem display = new DeviceItem(device,
					device.getDisplayString());
			deviceRemoved(display);
		}

		public void deviceAdded(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (!mDevList.contains(di)) {
						mDevList.add(di);
						mDevAdapter.notifyDataSetChanged();

					}
				}
			});
		}

		public void deviceRemoved(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {
					mDevList.remove(di);
					mDevAdapter.notifyDataSetChanged();
				}
			});
		}

		public void dmrAdded(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {
					if (!mDmrList.contains(di)) {
						mDmrList.add(di);
						mDmrDevAdapter.notifyDataSetChanged();
						if (di.getDevice().getDetails().getFriendlyName().contains(registerMediaPlayer)) {
							BaseApplication.dmrDeviceItem = di;
							BaseApplication.isLocalDmr = false;
						}
					}
				}
			});
		}

		public void dmrRemoved(final DeviceItem di) {
			runOnUiThread(new Runnable() {
				public void run() {
					mDmrList.remove(di);
					mDmrDevAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	private String[] imageThumbColumns = new String[]{
			MediaStore.Images.Thumbnails.IMAGE_ID,
			MediaStore.Images.Thumbnails.DATA};

	private void prepareMediaServer() {

			PrepareMedia mediaData = new PrepareMedia();
			mediaData.createMedia(DevicesActivity.this);
			ContentNode rootNode = ContentTree.getRootNode();
			// Video Container
			Container videoContainer = new Container();
			videoContainer.setClazz(new DIDLObject.Class("object.container"));
			videoContainer.setId(ContentTree.VIDEO_ID);
			videoContainer.setParentID(ContentTree.ROOT_ID);
			videoContainer.setTitle("Videos");
			videoContainer.setRestricted(true);
			videoContainer.setWriteStatus(WriteStatus.WRITABLE);
			videoContainer.setChildCount(0);

			rootNode.getContainer().addContainer(videoContainer);
			rootNode.getContainer().setChildCount(
					rootNode.getContainer().getChildCount() + 1);
			ContentTree.addNode(ContentTree.VIDEO_ID, new ContentNode(
					ContentTree.VIDEO_ID, videoContainer));

			Cursor cursor;
			String[] videoColumns = {MediaStore.Video.Media._ID,
					MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DATA,
					MediaStore.Video.Media.ARTIST,
					MediaStore.Video.Media.MIME_TYPE, MediaStore.Video.Media.SIZE,
					MediaStore.Video.Media.DURATION,
					MediaStore.Video.Media.RESOLUTION,
					MediaStore.Video.Media.DESCRIPTION};
			cursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
					videoColumns, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					String filePath = cursor.getString(cursor
							.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
					if (filePath.contains(PrepareMedia.videoRoot.toString())) {
						String id = ContentTree.VIDEO_PREFIX
								+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Video.Media._ID));
						String title = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
						String creator = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST));
						String mimeType = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE));
						long size = cursor.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
						long duration = cursor
								.getLong(cursor
										.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
						String resolution = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Video.Media.RESOLUTION));

						String description = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Video.Media.DESCRIPTION));

						Res res = new Res(new MimeType(mimeType.substring(0,
								mimeType.indexOf('/')), mimeType.substring(mimeType
								.indexOf('/') + 1)), size, "http://"
								+ mediaServer.getAddress() + "/" + id);

						res.setDuration(duration / (1000 * 60 * 60) + ":"
								+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
								+ (duration % (1000 * 60)) / 1000);
						res.setResolution(resolution);

						VideoItem videoItem = new VideoItem(id, ContentTree.VIDEO_ID,
								title, creator, res);

						// add video thumb Property
						String videoSavePath = ImageUtil.getSaveVideoFilePath(filePath,
								id);
						DIDLObject.Property albumArtURI = new DIDLObject.Property.UPNP.ALBUM_ART_URI(
								URI.create("http://" + mediaServer.getAddress()
										+ videoSavePath));
						Property[] properties = {albumArtURI};
						videoItem.addProperties(properties);
						videoItem.setDescription(description);
						videoContainer.addItem(videoItem);
						videoContainer
								.setChildCount(videoContainer.getChildCount() + 1);
						ContentTree.addNode(id,
								new ContentNode(id, videoItem, filePath));

						//ContentItem video = new ContentItem(); (videoItem, BaseApplication.upnpService)
						// Log.v(LOGTAG, "added video item " + title + "from " +
						// filePath);
					}
				} while (cursor.moveToNext());
			}

			// Audio Container
			Container audioContainer = new Container();
			audioContainer.setClazz(new DIDLObject.Class("object.container"));
			audioContainer.setId(ContentTree.AUDIO_ID);
			audioContainer.setParentID(ContentTree.ROOT_ID);
			audioContainer.setTitle("Audios");
			audioContainer.setRestricted(true);
			audioContainer.setWriteStatus(WriteStatus.WRITABLE);
			audioContainer.setChildCount(0);

			rootNode.getContainer().addContainer(audioContainer);
			rootNode.getContainer().setChildCount(
					rootNode.getContainer().getChildCount() + 1);
			ContentTree.addNode(ContentTree.AUDIO_ID, new ContentNode(
					ContentTree.AUDIO_ID, audioContainer));

			String[] audioColumns = {MediaStore.Audio.Media._ID,
					MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA,
					MediaStore.Audio.Media.ARTIST,
					MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.SIZE,
					MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM};
			cursor = managedQuery(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					audioColumns, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					String filePath = cursor.getString(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

					if (filePath.contains(PrepareMedia.audioRoot.toString())) {
						String id = ContentTree.AUDIO_PREFIX
								+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Audio.Media._ID));
						String title = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
						String creator = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
						String mimeType = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE));
						long size = cursor.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
						long duration = cursor
								.getLong(cursor
										.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
						String album = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
						Res res = null;
						try {
							res = new Res(new MimeType(mimeType.substring(0,
									mimeType.indexOf('/')), mimeType.substring(mimeType
									.indexOf('/') + 1)), size, "http://"
									+ mediaServer.getAddress() + "/" + id);
						} catch (Exception e) {
							Log.w(LOGTAG, "Exception1", e);
						}

						if (null == res) {
							break;
						}
						res.setDuration(duration / (1000 * 60 * 60) + ":"
								+ (duration % (1000 * 60 * 60)) / (1000 * 60) + ":"
								+ (duration % (1000 * 60)) / 1000);

						// Music Track must have `artist' with role field, or
						// DIDLParser().generate(didl) will throw nullpointException
						MusicTrack musicTrack = new MusicTrack(id,
								ContentTree.AUDIO_ID, title, creator, album,
								new PersonWithRole(creator, "Performer"), res);
						audioContainer.addItem(musicTrack);
						audioContainer
								.setChildCount(audioContainer.getChildCount() + 1);
						ContentTree.addNode(id, new ContentNode(id, musicTrack,
								filePath));

						// Log.v(LOGTAG, "added audio item " + title + "from " +
						// filePath);
					}
				} while (cursor.moveToNext());
			}

			// get image thumbnail
			Cursor thumbCursor = this.managedQuery(
					MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
					imageThumbColumns, null, null, null);
			HashMap<Integer, String> imageThumbs = new HashMap<Integer, String>();
			if (null != thumbCursor && thumbCursor.moveToFirst()) {
				do {
					imageThumbs
							.put(thumbCursor.getInt(0), thumbCursor.getString(1));
				} while (thumbCursor.moveToNext());

				if (Integer.parseInt(Build.VERSION.SDK) < 14) {
					thumbCursor.close();
				}
			}

			// Image Container

			Container imageContainer = new Container();
			imageContainer.setClazz(new DIDLObject.Class("object.container"));
			imageContainer.setId(ContentTree.IMAGE_ID);
			imageContainer.setParentID(ContentTree.ROOT_ID);
			imageContainer.setTitle("Images");
			imageContainer.setRestricted(true);
			imageContainer.setWriteStatus(WriteStatus.WRITABLE);
			imageContainer.setChildCount(0);

			rootNode.getContainer().addContainer(imageContainer);
			rootNode.getContainer().setChildCount(
					rootNode.getContainer().getChildCount() + 1);
			ContentTree.addNode(ContentTree.IMAGE_ID, new ContentNode(
					ContentTree.IMAGE_ID, imageContainer));

			String[] imageColumns = {MediaStore.Images.Media._ID,
					MediaStore.Images.Media.TITLE, MediaStore.Images.Media.DATA,
					MediaStore.Images.Media.MIME_TYPE,
					MediaStore.Images.Media.SIZE,
					MediaStore.Images.Media.DESCRIPTION};
			cursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
					imageColumns, null, null, MediaStore.Images.Media.DATA);

			Container typeContainer = null;
			if (cursor.moveToFirst()) {
				do {
					String filePath = cursor.getString(cursor
							.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
					if (filePath.contains(PrepareMedia.imageRoot.toString())) {
						int imageId = cursor.getInt(cursor
								.getColumnIndex(MediaStore.Images.Media._ID));
						String id = ContentTree.IMAGE_PREFIX
								+ cursor.getInt(cursor
								.getColumnIndex(MediaStore.Images.Media._ID));
						String title = cursor.getString(cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.TITLE));
						String creator = "unkown";

						String mimeType = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
						long size = cursor.getLong(cursor
								.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));

						String description = cursor
								.getString(cursor
										.getColumnIndexOrThrow(MediaStore.Images.Media.DESCRIPTION));

						String url = "http://" + mediaServer.getAddress() + "/"
								+ filePath;
						Res res = new Res(new MimeType(mimeType.substring(0,
								mimeType.indexOf('/')), mimeType.substring(mimeType
								.indexOf('/') + 1)), size, "http://"
								+ mediaServer.getAddress() + "/" + id);

						ImageItem imageItem = new ImageItem(id,
								ContentTree.IMAGE_ID, title, creator,
								res);

						// set albumArt Property
						if (imageThumbs.containsKey(imageId)) {
							String thumb = imageThumbs.get(imageId);
							Log.i(LOGTAG, " image thumb:" + thumb);
							// set albumArt Property
							DIDLObject.Property albumArtURI = new DIDLObject.Property.UPNP.ALBUM_ART_URI(
									URI.create("http://" + mediaServer.getAddress()
											+ thumb));
							DIDLObject.Property[] properties = {albumArtURI};
							imageItem.addProperties(properties);
						}
						imageItem.setDescription(description);
						imageContainer.addItem(imageItem);
						imageContainer
								.setChildCount(imageContainer.getChildCount() + 1);
						ContentTree.addNode(id,
								new ContentNode(id, imageItem, filePath));

						Log.v(LOGTAG, "added image item " + title + "from " + filePath);
					}

				} while (cursor.moveToNext());
			}
		serverPrepared = true;
	}

	class DevAdapter extends ArrayAdapter<DeviceItem> {

		private static final String TAG = "DeviceAdapter";

		private LayoutInflater mInflater;

		public int dmrPosition = 0;

		private List<DeviceItem> deviceItems;

		public DevAdapter(Context context, int textViewResourceId,
						  List<DeviceItem> objects) {
			super(context, textViewResourceId, objects);
			this.mInflater = ((LayoutInflater) context
					.getSystemService("layout_inflater"));
			this.deviceItems = objects;
		}

		public int getCount() {
			return this.deviceItems.size();
		}

		public DeviceItem getItem(int paramInt) {
			return this.deviceItems.get(paramInt);
		}

		public long getItemId(int paramInt) {
			return paramInt;
		}

		public View getView(int position, View view, ViewGroup viewGroup) {

			DevHolder holder;
			if (view == null) {
				view = this.mInflater.inflate(R.layout.dmr_item, null);
				holder = new DevHolder();
				holder.filename = ((TextView) view
						.findViewById(R.id.dmr_name_tv));
				holder.checkBox = ((CheckBox) view.findViewById(R.id.dmr_cb));
				view.setTag(holder);
			} else {
				holder = (DevHolder) view.getTag();
			}
			DeviceItem item = (DeviceItem) this.deviceItems.get(position);
			holder.filename.setText(item.toString());
			holder.filename.setText(item.toString());
			if (null != BaseApplication.deviceItem
					&& BaseApplication.deviceItem.equals(item)) {
				holder.checkBox.setChecked(true);
			} else if (null != BaseApplication.dmrDeviceItem
					&& BaseApplication.dmrDeviceItem.equals(item)) {
				holder.checkBox.setChecked(true);
				reconnection = true;
			} else {
				holder.checkBox.setChecked(false);
			}
			return view;
		}

		public final class DevHolder {
			public TextView filename;
			public CheckBox checkBox;
		}
	}

	private BroadcastReceiver myWifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {

			int status = CommonUtil.checkNetState(context);
			if (status == 2 && reconnection == true) {
				mDmrDevAdapter.notifyDataSetChanged();
				Toast.makeText(mContext,"Wifi connected", Toast.LENGTH_SHORT);
				ContentActivity.restart();
				IndexActivity.setContentPageSelected();
			} else if (status == 1) {
				mDmrList.remove(selectedDisplayDevice);
				mDmrDevAdapter.notifyDataSetChanged();
				mHandler.sendEmptyMessage(INTIMATE_DISCONNECTION_TO_SNAPBILLING);
			}
		}
	};
}
