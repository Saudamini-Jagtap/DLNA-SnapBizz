package com.zxt.dlna.activity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import java.util.Random;

//import org.bytedeco.javacpp.opencv_core;
//import org.bytedeco.javacv.FFmpegFrameRecorder;
//import org.bytedeco.javacv.Frame;
//import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacpp.annotation.Const;
import org.eclipse.jetty.server.ResourceCache;
import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.model.types.csv.CSVUnsignedIntegerTwoBytes;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.PersonWithRole;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.WriteStatus;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.ImageItem;
import org.fourthline.cling.support.model.item.Item;
import org.fourthline.cling.support.model.item.MusicTrack;
import org.fourthline.cling.support.model.item.VideoItem;
import org.seamless.util.MimeType;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.zxt.dlna.R;


import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.display.RoundedBitmapDisplayer;
import com.zxt.dlna.application.BaseApplication;
import com.zxt.dlna.application.ConfigData;
import com.zxt.dlna.application.VisibilityEntry;
import com.zxt.dlna.dmc.GenerateXml;
import com.zxt.dlna.dmp.ContentItem;
import com.zxt.dlna.dmp.DeviceItem;
import com.zxt.dlna.dmp.GPlayer;
import com.zxt.dlna.dmp.ImageDisplay;
import com.zxt.dlna.dms.ContentBrowseActionCallback;
import com.zxt.dlna.dms.ContentNode;
import com.zxt.dlna.dms.ContentTree;
import com.zxt.dlna.dms.MediaServer;
import com.zxt.dlna.service.Constants;
import com.zxt.dlna.service.MediaService;
import com.zxt.dlna.util.CommonUtil;
import com.zxt.dlna.util.ImageUtil;
import com.zxt.dlna.util.PrepareMedia;

import static com.zxt.dlna.R.drawable.player_bt_phone_vlist_press;
import static com.zxt.dlna.R.drawable.titlebar;
import static com.zxt.dlna.application.BaseApplication.mContext;
import static com.zxt.dlna.application.BaseApplication.mediaServer;
import static com.zxt.dlna.dmp.GPlayer.LOGTAG;
//import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

public class ContentActivity extends Activity {

	private static boolean restart = false;

	public static final int CONTENT_GET_FAIL = 0;

	public static final int CONTENT_GET_SUC = 1;

	public static final int CONTENT_GET_UPDATE = 2;

	private ListView mContentLv;

	private TextView mTitleView;

	private ProgressBar mProgressBarPreparing;

	private ArrayList<ContentItem> mContentList = new ArrayList<ContentItem>();

	private ContentAdapter mContentAdapter;

	private BaseApplication mBaseApplication;

	private AndroidUpnpService upnpService;

	private String currentContentFormatMimeType = "";

	private Context mContext;

	private Map<Integer, ArrayList<ContentItem>> mSaveDirectoryMap;

	private Integer mCounter = 0;

	private String mLastDevice;

	private String mThumbUri;

	DisplayImageOptions options;

	private int currentposition = 0;

	private Button mUpdateButton;

	protected ImageLoader imageLoader = ImageLoader.getInstance();

	MediaService serviceReference;

	boolean isBound = false;

	static boolean update = false;

	private MediaServer mediaServer;


	private Container rootContainer;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case CONTENT_GET_FAIL: {

				break;
			}
			case CONTENT_GET_SUC: {

				mContentAdapter.notifyDataSetChanged();
				mProgressBarPreparing.setVisibility(View.GONE);

				mCounter++;

				ArrayList<ContentItem> tempContentList = new ArrayList<ContentItem>();
				tempContentList.addAll(mContentList);
				mSaveDirectoryMap.put(mCounter - 1, tempContentList);
				break;
			}

				case CONTENT_GET_UPDATE:{
						mContentAdapter.notifyDataSetChanged();
						mProgressBarPreparing.setVisibility(View.GONE);
						mUpdateButton.setBackgroundDrawable(getResources().getDrawable(titlebar));
						mUpdateButton.setEnabled(true);
						initData();
					}
			default:
				break;
			}
			super.handleMessage(msg);
		}

	};

	ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			serviceReference = ((MediaService.MyLocalBinder)service).getService();
			serviceReference.setParentActivity(ContentActivity.this);
			isBound = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			serviceReference = null;
			isBound = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content_lay);

		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.icon_image)
				.showImageForEmptyUri(R.drawable.ic_empty)
				.showImageOnFail(R.drawable.ic_error).cacheInMemory()
				.cacheOnDisc().displayer(new RoundedBitmapDisplayer(20))
				.build();

		Intent localIntent =new Intent(getBaseContext(),
                MediaService.class);
        localIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        getApplication().startService(localIntent);
        getApplication().bindService(new Intent(getBaseContext(),
                        MediaService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

		initView();
	}
	@Override
	protected void onNewIntent(Intent intent) {
		if (intent != null)
			setIntent(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mContext = ContentActivity.this;
		mBaseApplication = (BaseApplication) this.getApplication();
		if (null == mBaseApplication.deviceItem) {
			Toast.makeText(mContext, R.string.not_select_dev,
					Toast.LENGTH_SHORT).show();
		} else if (null == mLastDevice || "" == mLastDevice
				|| mLastDevice != mBaseApplication.deviceItem.toString()) {
			initData();
		} else {

		}
	}

	private void initView() {
		mTitleView = (TextView) findViewById(R.id.dev_name_tv);
		mProgressBarPreparing = (ProgressBar) findViewById(R.id.player_prepairing);
		mContentLv = (ListView) findViewById(R.id.content_list);
		mContentAdapter = new ContentAdapter(ContentActivity.this, mContentList);
		mContentLv.setAdapter(mContentAdapter);
		mContentLv.setOnItemClickListener(contentItemClickListener);
		mUpdateButton = (Button) findViewById(R.id.update_Download);
		mUpdateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mProgressBarPreparing.setVisibility(View.VISIBLE);
				mUpdateButton.setBackgroundDrawable(getResources().getDrawable(player_bt_phone_vlist_press));
				mUpdateButton.setEnabled(false);
				mContentList.clear();
				mContentAdapter.notifyDataSetChanged();
				new Thread(new Runnable() {
					@Override
					public void run() {
						mediaServer = BaseApplication.mediaServer;
						prepareMediaServer();
						mHandler.sendEmptyMessage(CONTENT_GET_UPDATE);
					}
				}).start();
			}
		});
	}
	private void initData() {

		mCounter = 0;
		if (null == mSaveDirectoryMap) {
			mSaveDirectoryMap = new HashMap<Integer, ArrayList<ContentItem>>();
		} else {
			mSaveDirectoryMap.clear();
		}
		upnpService = mBaseApplication.upnpService;
		mTitleView.setText(mBaseApplication.deviceItem.toString());
		Device device = mBaseApplication.deviceItem.getDevice();
		Service service = device.findService(new UDAServiceType(
				"ContentDirectory"));
		upnpService.getControlPoint().execute(
				new ContentBrowseActionCallback(ContentActivity.this,
						service, createRootContainer(service), mContentList,
						mHandler));

		mLastDevice = mBaseApplication.deviceItem.toString();
	}

	protected Container createRootContainer(Service service) {
		Container rootContainer = new Container();
		rootContainer.setId("0");
		rootContainer.setTitle("Content Directory on "
				+ service.getDevice().getDisplayString());
		return rootContainer;
	}

	private void setAnimation() {

		AnimationSet set = new AnimationSet(false);
		Animation animation = new AlphaAnimation(0, 1); // AlphaAnimation
														// 控制渐变透明的动画效果
		animation.setDuration(500); // 动画时间毫秒数
		set.addAnimation(animation); // 加入动画集合

		animation = new TranslateAnimation(1, 13, 10, 50); // ScaleAnimation
															// 控制尺寸伸缩的动画效果
		animation.setDuration(300);
		set.addAnimation(animation);

		animation = new RotateAnimation(30, 10); // TranslateAnimation
													// 控制画面平移的动画效果
		animation.setDuration(300);
		set.addAnimation(animation);

		animation = new ScaleAnimation(5, 0, 2, 0); // RotateAnimation
													// 控制画面角度变化的动画效果
		animation.setDuration(300);
		set.addAnimation(animation);

		LayoutAnimationController controller = new LayoutAnimationController(
				set, 1);

		mContentLv.setLayoutAnimation(controller); // ListView 设置动画效果

	}
	OnItemClickListener contentItemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {
			// TODO Auto-generated method stub
			ContentItem content = mContentList.get(position);
			if (content.isContainer()) {
				mProgressBarPreparing.setVisibility(View.VISIBLE);
				upnpService.getControlPoint()
						.execute(
								new ContentBrowseActionCallback(
										ContentActivity.this, content
												.getService(), content
												.getContainer(), mContentList,
										mHandler));
			} else {
				currentposition = position;
				MimeType localMimeType = content.getItem().getResources()
						.get(0).getProtocolInfo().getContentFormatMimeType();
				if (null == localMimeType) {
					return;
				}
				String type = localMimeType.getType();
				if (null == type) {
					return;
				}
				currentContentFormatMimeType = localMimeType.toString();

				Intent intent = new Intent();
				if (type.equals("image")) {
					ConfigData.photoPosition = position;
					jumpToImage(content);
				} else {
					jumpToControl(content);
				}
			}
		}
	};

	private void jumpToControl(ContentItem localContentItem) {
		serviceReference.stopSlideShow();
		Bundle localBundle = new Bundle();
		//Intent localIntent = new Intent("com.transport.info");
		localBundle.putString("name", localContentItem.toString());
		localBundle.putString("playURI", localContentItem.getItem()
				.getFirstResource().getValue());
		localBundle.putString("currentContentFormatMimeType",
				currentContentFormatMimeType);
		localBundle.putBoolean("restart",true);
		try {
			localBundle.putString("metaData",
					new GenerateXml().generate(localContentItem));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Intent localIntent = new Intent(ContentActivity.this,MediaService.class);
		localIntent.setAction(Constants.ACTION.STARTVIDEO_ACTION);
		localIntent.putExtras(localBundle);
		getApplication().startService(localIntent);

	}

	private void jumpToImage(ContentItem localContentItem) {
		serviceReference.stopVideo();
		Intent localIntent = new Intent(ContentActivity.this,
				MediaService.class);
		localIntent.putExtra("name", localContentItem.toString());
		localIntent.putExtra("playURI", localContentItem.getItem()
				.getFirstResource().getValue());
		localIntent.putExtra("currentContentFormatMimeType",
				currentContentFormatMimeType);
		try {
			localIntent.putExtra("metaData",
					new GenerateXml().generate(localContentItem));
		} catch (Exception e) {
			e.printStackTrace();
		}
		localIntent.setAction(Constants.ACTION.STARTSLIDESHOW_ACTION);
		getApplication().startService(localIntent);
	}

	private String getThumbUri(ContentItem contentItem) {
		String thumbUri = null;
		int i = contentItem.getItem().getProperties().size();
		for (int j = 0; j < i; j++) {
			if (null != contentItem.getItem()
					&& null != contentItem.getItem().getProperties()
					&& null != contentItem.getItem().getProperties().get(j)
					&& ((DIDLObject.Property) contentItem.getItem()
							.getProperties().get(j)).getDescriptorName()
							.equals("albumArtURI")) {

				thumbUri = ((DIDLObject.Property) contentItem.getItem()
						.getProperties().get(j)).getValue().toString();
				break;
			}
		}

		return thumbUri;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mCounter > 1) {
				mSaveDirectoryMap.remove(mCounter - 1);
				mCounter--;
				mContentList.clear();
				mContentList.addAll(mSaveDirectoryMap.get(mCounter - 1));
				mContentAdapter.notifyDataSetChanged();
				return true;
			}else{
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	class ContentAdapter extends BaseAdapter {

		private static final String TAG = "ContentAdapter";

		private Context context;

		private LayoutInflater mInflater;

		private Bitmap imageIcon;

		private Bitmap videoIcon;

		private Bitmap audioIcon;

		private Bitmap folderIcon;

		public int dmrPosition = 0;

		private ArrayList<ContentItem> mDeviceItems;

		private ImageLoadingListener animateFirstListener = new AnimateFirstDisplayListener();

		public ContentAdapter(Context paramContext,
				ArrayList<ContentItem> paramArrayList) {
			this.mInflater = ((LayoutInflater) paramContext
					.getSystemService("layout_inflater"));
			this.context = paramContext;
			this.mDeviceItems = paramArrayList;
			imageIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.icon_image);
			videoIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.icon_video);
			audioIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.icon_audio);
			folderIcon = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.icon_folder);
		}

		public int getCount() {
			return this.mDeviceItems.size();
		}

		public Object getItem(int paramInt) {
			return this.mDeviceItems.get(paramInt);
		}

		public long getItemId(int paramInt) {
			return paramInt;
		}

		public View getView(int paramInt, View paramView,
				ViewGroup paramViewGroup) {
			final ContentHolder localHolder;
			if (paramView == null) {
				paramView = this.mInflater.inflate(R.layout.content_item, null);
				localHolder = new ContentHolder();
				localHolder.filename = (TextView) paramView
						.findViewById(R.id.content_title_tv);
				localHolder.folder = (ImageView) paramView
						.findViewById(R.id.icon_folder);
				localHolder.arrow = (ImageView) paramView
						.findViewById(R.id.icon_arrow);
				paramView.setTag(localHolder);
			} else {
				localHolder = (ContentHolder) paramView.getTag();
			}
			ContentItem contentItem = (ContentItem) this.mDeviceItems
					.get(paramInt);

			localHolder.filename.setText(contentItem.toString());

			if (!contentItem.isContainer()) {
				String imageUrl = null;
				if (null != contentItem.getItem().getResources().get(0)
						.getProtocolInfo().getContentFormatMimeType()) {
					String type = contentItem.getItem().getResources().get(0)
							.getProtocolInfo().getContentFormatMimeType()
							.getType();
					if (type.equals("image")) {
						localHolder.folder.setImageBitmap(imageIcon);
						// if is image, display it
						imageUrl = contentItem.getItem().getFirstResource()
								.getValue();
					} else if (type.equals("video")) {
						localHolder.folder.setImageBitmap(videoIcon);
					} else if (type.equals("audio")) {
						localHolder.folder.setImageBitmap(audioIcon);
					}

				}

				int i = contentItem.getItem().getProperties().size();
				for (int j = 0; j < i; j++) {
					if (null != contentItem.getItem()
							&& null != contentItem.getItem().getProperties()
							&& null != contentItem.getItem().getProperties()
									.get(j)
							&& ((DIDLObject.Property) contentItem.getItem()
									.getProperties().get(j))
									.getDescriptorName().equals("albumArtURI")) {

						imageUrl = ((DIDLObject.Property) contentItem.getItem()
								.getProperties().get(j)).getValue().toString();
						break;
					}
				}
				imageLoader.displayImage(imageUrl, localHolder.folder, options,
						animateFirstListener);
				localHolder.arrow.setVisibility(View.GONE);
			} else {
				localHolder.folder.setImageBitmap(folderIcon);
				localHolder.arrow.setVisibility(View.VISIBLE);

			}
			return paramView;
		}

		class ContentHolder {

			public TextView filename;

			public ImageView folder;

			public ImageView arrow;

			public ContentHolder() {
			}
		}

	}

	private static class AnimateFirstDisplayListener extends
			SimpleImageLoadingListener {

		static final List<String> displayedImages = Collections
				.synchronizedList(new LinkedList<String>());

		@Override
		public void onLoadingComplete(String imageUri, View view,
				Bitmap loadedImage) {
			if (loadedImage != null) {
				ImageView imageView = (ImageView) view;
				boolean firstDisplay = !displayedImages.contains(imageUri);
				if (firstDisplay) {
					FadeInBitmapDisplayer.animate(imageView, 500);
					displayedImages.add(imageUri);
				}
			}
		}
	}
	public static void restart() {
		restart = true;
	}
	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(upnpService != null){
			upnpService = null;
		}
		getApplication().unbindService(serviceConnection);
		serviceReference = null;
	}
	private String[] imageThumbColumns = new String[]{
			MediaStore.Images.Thumbnails.IMAGE_ID,
			MediaStore.Images.Thumbnails.DATA};

	private void prepareMediaServer() {
		//TODO: uncomment for final
		PrepareMedia mediaData = new PrepareMedia();
		mediaData.updateMedia(ContentActivity.this);
		ContentTree.delete();
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
					DIDLObject.Property[] properties = {albumArtURI};
					videoItem.addProperties(properties);
					videoItem.setDescription(description);
					videoContainer.addItem(videoItem);
					videoContainer
							.setChildCount(videoContainer.getChildCount() + 1);
					ContentTree.addNode(id,
							new ContentNode(id, videoItem, filePath));
				}
			} while (cursor.moveToNext());
		}
		// audio cotainer
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
	}
}
