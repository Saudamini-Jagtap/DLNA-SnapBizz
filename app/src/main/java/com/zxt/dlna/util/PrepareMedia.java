package com.zxt.dlna.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

import com.zxt.dlna.activity.SettingActivity;
import com.zxt.dlna.application.VisibilityEntry;
import com.zxt.dlna.dms.ContentTree;

import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;
import org.bytedeco.javacv.OpenCVFrameConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static com.zxt.dlna.util.CommonUtil.deleteRecursive;
import static org.bytedeco.javacpp.opencv_core.cvReleaseImage;
import static org.bytedeco.javacpp.opencv_imgcodecs.cvLoadImage;

public class PrepareMedia {

    private static final int MAX_IMAGE_SIZE = 1000; // in KB's

    private static final int MAX_IMAGE_HEIGHT = 1080;

    private static final int MAX_IMAGE_WIDHT = 1980;

    private static final int MAX_DURATION__OF_VIDEO = 1800; // value in sec
    // 12min = 720sec if number of slides are 48
    // 30min = 1800sec for 48 slides
    // 1hr = 3600sec

    protected static final int CAMPAIGN_DATA_READY = 5000;

    private int mMaxWidth;
    private int mMaxHeight;
    private DisplayMetrics metrics;

    /**
     * declaration of the visibilityEntry class
     **/
    private static VisibilityEntry visibleCampaignEntry;
    private boolean campaignSlidesReady = false;
    private boolean campaignVideosReady = false;
    private boolean campaignAudiosReady = false;


    /**
     * constants
     **/
    protected final int MAX_NUM_SLIDES = 48;
    protected final int MAX_NUM_MYSTORE_SLIDES = 1;
    protected final int MAX_NUM_CAMPAIGN_SLIDES = 40;
    protected final int MAX_NUM_LOCAL_SLIDES = 8;

    protected final int BRAND_CAMPAIGN_IMAGE_TYPE = 1;
    protected final int LOCAL_PRODUCT_IMAGE_TYPE = 2;
    protected final int MYSTORE_IMAGE_TYPE = 3;
    protected final int CAMPAIGN_VIDEO_TYPE = 4;

    /**
     * variables
     **/
    private int mNumberOfImages = 0;
    private int mNumberOfBrandCampaignImages = 0;
    private int mNumberOfLocalOfferImages = 0;
    private int mNumberOfMyStoreImages = 0;

    /**
     * replaced by Snap-billing database images
     **/
    private List<VisibilityEntry> mBrandCampaignImageList = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mLocalOffersImageList = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mMyStoreImageList = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mLocalCampaignSlides = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mCampaignSlides = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mCampaignVideos = new ArrayList<VisibilityEntry>();
    private List<VisibilityEntry> mFinalCampaignToBeDisplayed = new ArrayList<VisibilityEntry>();

    private Context mContext;

    /**
     * directory of the campaign slides to be shown
     **/
    public static final File imageRoot = new File(Environment.getExternalStorageDirectory(), "campaignSlidesToDisplay");

    /**
     * directory of the campaign videos to be shown
     **/
    public static final File videoRoot = new File(Environment.getExternalStorageDirectory(), "campaignVideosToDisplay");

    /**
     * directory of the campaign music to be shown
     **/
    public static final File audioRoot = new File(Environment.getExternalStorageDirectory(), "campaignAudiosToDisplay");

    /**
     * constructor
     **/
    public PrepareMedia(Context context){
        this.mContext = context;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CAMPAIGN_DATA_READY:
                    if (campaignSlidesReady && campaignVideosReady /*&& campaignAudiosReady*/) {
                        combineClipsAndVideos();
                        append_videos(mContext);
                        campaignSlidesReady = false;
                        campaignVideosReady = false;
                        campaignAudiosReady = false;
                    }
                    break;
            }
        }
    };

    /**
     * Constructor
     **/
    public void createMedia(){

        visibleCampaignEntry = new VisibilityEntry();
        List<VisibilityEntry> mAllCampaignDataList = new ArrayList<VisibilityEntry>();
        mAllCampaignDataList =  visibleCampaignEntry.getAllCampaignData();
        sortCampaignData(mAllCampaignDataList);

        if(!imageRoot.exists() || imageRoot.listFiles().length <=0 )
            new Thread(new Runnable() {
                @Override
                public void run() {
                    prepareCampaignSlides(mContext);
                    campaignSlidesReady = true;
                    mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
                }
            }).start();

        if(!videoRoot.exists() || videoRoot.listFiles().length <=0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    prepareCampaignVideos();
                    campaignVideosReady = true;
                    mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
                }
            }).start();
        }
        if(!audioRoot.exists() || audioRoot.listFiles().length <=0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    prepareCampaignAudios();
                    campaignAudiosReady = true;
                    //TODO: uncomment when audio is required
                    //mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
                }
            }).start();
        }
    }

    /**
     * use this method whenever update needed
     **/
    public void updateMedia(){

        List<VisibilityEntry> mAllCampaignDataList = visibleCampaignEntry.getAllCampaignData();
        sortCampaignData(mAllCampaignDataList);

        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareCampaignSlides(mContext);
                campaignSlidesReady = true;
                mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareCampaignVideos();
                campaignVideosReady = true;
                mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                prepareCampaignAudios();
                campaignAudiosReady = true;
                //TODO: uncomment when audio is required
                //mHandler.sendEmptyMessage(CAMPAIGN_DATA_READY);
            }
        }).start();
    }

    /** fetching the campaign images from snapbilling application
     * and preparing the slides for the slide show
     * and saving the slides to device storage
     **/
    public void prepareCampaignSlides(Context context) {
        //campaign images from the database
        mCampaignSlides = prepareSlides();
        //saveCampaignSlides(mCampaignSlides, context);
    }

    /** sort the images into 3 categories :
     *	1. campaign images
     *	2. local images
     *	3. myStore images
     **/
    private void sortCampaignData(List<VisibilityEntry> allImages) {
        for (VisibilityEntry data : allImages) {
            switch (data.getCampaignType()) {
                case BRAND_CAMPAIGN_IMAGE_TYPE:
                    mBrandCampaignImageList.add(data);
                    break;
                case LOCAL_PRODUCT_IMAGE_TYPE:
                    mLocalOffersImageList.add(data);
                    break;
                case MYSTORE_IMAGE_TYPE:
                    mMyStoreImageList.add(data);
                    break;
                case CAMPAIGN_VIDEO_TYPE:
                    mCampaignVideos.add(data);
            }
        }
    }

    /** preparing the slideshow for the campaigning
     *
     * 	Number of slides: 48
     * 	Campaign Slides: 40 full screen slides
     *	Local Slides: 8 (2 images/slide ) – if there are less than 40 campaign slides it can be replaced with local slides.
     *	My Store Page: 1 full screen slide
     *
     **/
    public List<VisibilityEntry> prepareSlides() {
        List<VisibilityEntry> preaparedSlides = new ArrayList<VisibilityEntry>();
        if (mMyStoreImageList.size() >= 1) {
            preaparedSlides.add(resizeImages(mMyStoreImageList.get(0)));
            mNumberOfMyStoreImages = 1;
            mNumberOfBrandCampaignImages = (mBrandCampaignImageList.size() >= (MAX_NUM_CAMPAIGN_SLIDES - 1)) ? (MAX_NUM_CAMPAIGN_SLIDES - 1) : mBrandCampaignImageList.size();
        } else {
            mNumberOfMyStoreImages = 0;
            mNumberOfBrandCampaignImages = (mBrandCampaignImageList.size() >= MAX_NUM_CAMPAIGN_SLIDES) ? MAX_NUM_CAMPAIGN_SLIDES : mBrandCampaignImageList.size();

        }
        for (int i = 0; i < mNumberOfBrandCampaignImages; i++) {
            preaparedSlides.add(resizeImages(mBrandCampaignImageList.get(i)));
        }
        mLocalCampaignSlides = mergeLocalCampaignImages(mLocalOffersImageList);
        for (VisibilityEntry temp : mLocalCampaignSlides) {
            if (preaparedSlides.size() < MAX_NUM_SLIDES) {
                preaparedSlides.add(temp);
            }
        }
        return preaparedSlides;
    }

    /**
     * resize the resolution of the slide images if not at required
     **/
    public VisibilityEntry resizeImages(VisibilityEntry images) {
        Bitmap firstImage = BitmapFactory.decodeFile(images.getCampaignDataPath());
        int canvasWidth = MAX_IMAGE_WIDHT;
        int canvasHeight = MAX_IMAGE_HEIGHT;
        Bitmap nre = Bitmap.createScaledBitmap(firstImage, canvasWidth, canvasHeight, false);
        return convertBitmaptoSlide(nre, images);
    }

    /**
     * creating a local slides list
     **/
    public List<VisibilityEntry> mergeLocalCampaignImages(List<VisibilityEntry> localOfferImagesList) {
        List<VisibilityEntry> mergedImageList = new ArrayList<VisibilityEntry>();
        int i = 0;
        boolean oddImageCount = false;
        Random r = new Random();
        mNumberOfLocalOfferImages = localOfferImagesList.size();
        if(mNumberOfLocalOfferImages % 2 != 0){
            oddImageCount = true;
        }
        Collections.sort(localOfferImagesList, new SortBySlotNumber());

        for (i = 0; i < mNumberOfLocalOfferImages; i+= 2) {
            if ((oddImageCount == true) && (i == 0)) {
                mergedImageList.add(resizeImages(localOfferImagesList.get(i)));
                i += 1;
            }
            VisibilityEntry first = localOfferImagesList.get(i);
            VisibilityEntry second = localOfferImagesList.get((i + 1));
            Bitmap firstImage = BitmapFactory.decodeFile(first.getCampaignDataPath());
            Bitmap secondImage = BitmapFactory.decodeFile(second.getCampaignDataPath());
            Bitmap mergedImages = createSingleImageFrom2Images(firstImage, secondImage);
            mergedImageList.add((convertBitmaptoSlide(mergedImages, first)));
        }
//        if (mNumberOfLocalOfferImages % 2 != 0 )
//            oddImageCount = true;
//        for (i = 0; i < mNumberOfLocalOfferImages; i += 2) {
//            if ((oddImageCount == true) && (i == 0)) {
//                mergedImageList.add(resizeImages(localOfferImagesList.get(i), LOCAL_PRODUCT_IMAGE_TYPE, localOfferImagesList.get(i).getCampaignID()));
//                i += 1;
//            }
//            VisibilityEntry first = localOfferImagesList.get(i);
//            VisibilityEntry second = localOfferImagesList.get((i + 1));
//            Bitmap firstImage = BitmapFactory.decodeFile(first.getCampaignDataPath());
//            Bitmap secondImage = BitmapFactory.decodeFile(second.getCampaignDataPath());
//            Bitmap mergedImages = createSingleImageFrom2Images(firstImage, secondImage);
//            mergedImageList.add(new VisibilityEntry(convertBitmaptoSlide(mergedImages, i).getAbsolutePath(), LOCAL_PRODUCT_IMAGE_TYPE, r.nextInt(200)));
//        }
        return mergedImageList;
    }

    public DisplayMetrics getScreenResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        // getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    /**
     * merge two local images into single bitmap image
     **/
    private Bitmap createSingleImageFrom2Images(Bitmap firstImage, Bitmap secondImage) {
        int canvasWidth = MAX_IMAGE_WIDHT;
        int canvasHeight = MAX_IMAGE_HEIGHT;
        int scaledHeight;
        int scaledWidth;
        float hRation = 1;
        float vRation = 1;

        //resizing the first image
        if (canvasHeight < firstImage.getHeight()) {
            scaledHeight = (int) canvasHeight;
        } else {
            hRation = (float) (canvasHeight) / (float) firstImage.getHeight();
            scaledHeight = (int) (hRation * firstImage.getHeight());
        }
        if (canvasWidth < firstImage.getWidth()) {
            scaledWidth = canvasWidth / 2;
        } else {
            vRation = (float) (canvasWidth / 2) / (float) firstImage.getWidth();
            scaledWidth = (int) (vRation * firstImage.getWidth());
        }

        Bitmap scaleFstImage = Bitmap.createScaledBitmap(firstImage, scaledWidth, scaledHeight, false);

        //resizing the second image
        if (canvasHeight < secondImage.getHeight()) {
            scaledHeight = (int) canvasHeight;
        } else {
            hRation = (float) (canvasHeight) / (float) secondImage.getHeight();
            scaledHeight = (int) (hRation * secondImage.getHeight());
        }
        if (canvasWidth < firstImage.getWidth()) {
            scaledWidth = canvasWidth / 2;
        } else {
            vRation = (float) (canvasWidth / 2) / (float) secondImage.getWidth();
            scaledWidth = (int) (vRation * secondImage.getWidth());
        }
        Bitmap scaleSecImage = Bitmap.createScaledBitmap(secondImage, scaledWidth, scaledHeight, false);

        Bitmap result = Bitmap.createBitmap(canvasWidth, canvasHeight, firstImage.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(scaleFstImage, 0, 0, null);
        canvas.drawBitmap(scaleSecImage, scaleFstImage.getWidth(), 0f, null);
        return result;
    }

    /**
     * converting image formed by merging two local images into slide/file
     **/
    public VisibilityEntry convertBitmaptoSlide(Bitmap image, VisibilityEntry entry) {
//        String root = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
//        final String tempMergedDirName = "mergedImages";
//        final File imageTemp = new File(Environment.getExternalStorageDirectory(), tempMergedDirName);
        File imageTemp = new File(entry.getCampaignDataPath());
        if (!imageTemp.exists()) {
            imageTemp.mkdirs();
        }
        File f = new File(imageRoot, "mergedfile_" + entry.getCampaignID() + ".jpeg");
        try {
            f.createNewFile();
        } catch (IOException e) {
            //Log.e(e.getMessage());
            System.out.println(e);
        }
        //Convert bitmap to byte array
        //Bitmap bitmap = your bitmap;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100 /*ignored for PNG*/, bos);
        byte[] bitmapdata = bos.toByteArray();

        /*write the bytes in file*/
        try {
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }
        // updated filepath
        entry.setCampaignDataPath(f.getAbsolutePath());

        return entry;
    }

    /**
     * saving all the campaign slides/files into the device storage
     **/
    public void saveCampaignSlides(List<VisibilityEntry> slides, Context context) {
        int i = 0;
        String root = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        if (!imageRoot.exists()) {
            imageRoot.mkdirs();
        } else {
            for (File child : imageRoot.listFiles()) {
                deleteRecursive(child);
                deleteImagefromGallery(child, context);
            }
        }
        for (VisibilityEntry temp : slides) {

            String newFileName = "slide_" + (++i) + ".jpeg";
            File sourceSlide = new File(temp.getCampaignDataPath());
            File destSlide = new File(imageRoot, newFileName);
            long fileSize = Integer.parseInt(String.valueOf(sourceSlide.length() / 1024));

            /* use this if need to compress the size of the image*/
//            if (fileSize > MAX_IMAGE_SIZE)
//                compressFileSize(sourceSlide,fileSize,(int)fileSize/MAX_IMAGE_SIZE,0);
            FileChannel source = null;
            FileChannel destination = null;
            try {
                source = new FileInputStream(sourceSlide).getChannel();

                destination = new FileOutputStream(destSlide).getChannel();
                if (destination != null && source != null) {
                    destination.transferFrom(source, 0, source.size());
                    addImageToGallery(destSlide.toString(), newFileName, source.size(), context);
                }
                if (source != null) {
                    source.close();
                }
                if (destination != null) {
                    destination.close();
                }
            } catch (FileNotFoundException e) {

            } catch (IOException e) {

            }
        }

        //deleteRecursive(new File(root + "mergedImages"));
        //remove when images imported from the Snap-billing application
        //deleteRecursive(new File(root + "campaignImages"));
    }

//    private void record(Context context) {
//
//        if(!videoRoot.exists()){
//            videoRoot.mkdir();
//        }
//        String video_output = videoRoot.getAbsolutePath().toString() + "/" + "CampaignSlideShow" + ".mp4";
//        String path = Environment.getExternalStorageDirectory().getPath() + "/campaignSlidesToDisplay"; // You can provide SD Card path here.
//        deleteVideofromGallery(video_output, context);
//        File folder = new File(path);
//        File[] listOfFiles = folder.listFiles();
//        opencv_core.IplImage iplimage = null;
//        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(video_output, 1270,720,2);
//        OpenCVFrameConverter.ToIplImage imgToFrame = new OpenCVFrameConverter.ToIplImage();
//
//        if (listOfFiles.length > 0) {
//            try {
//                recorder.setVideoCodec(13);// CODEC_ID_MPEG4 //CODEC_ID_MPEG1VIDEO
//                recorder.setFormat("mp4");
//                //http://stackoverflow.com/questions/14125758/javacv-ffmpegframerecorder-properties-explanation-needed
//                recorder.setFrameRate(25); // This is the frame rate for video. If you really want to have good video quality you need to provide large set of images.
//                recorder.setPixelFormat(0); // PIX_FMT_YUV420P
//
//                int slideTime = SettingActivity.getSlideTime(mContext);
//                int numbOfRepeat = MAX_DURATION__OF_VIDEO/(listOfFiles.length*slideTime);
//                recorder.start();
//                long startTime = System.currentTimeMillis();
//                for (int j = 0; j < listOfFiles.length; j++ ) {
//                    String files = "";
//                    if (listOfFiles[j].isFile()) {
//                        files = listOfFiles[j].getName();
//                        System.out.println(" j " + j + listOfFiles[j]);
//                    }
//                    String[] tokens = files.split("\\.(?=[^\\.]+$)");
//                    String name = tokens[0];
//                    opencv_core.IplImage iplImage = cvLoadImage(Environment.getExternalStorageDirectory().getPath() + "/campaignSlidesToDisplay/" + name + ".jpeg");
//                    Frame slideFrame = imgToFrame.convert(iplImage);
//                    //for (int i = 0; i < slideTime*25; i++) {
//                    long time = 12500L * (System.currentTimeMillis() - startTime);
//                    if (time > recorder.getTimestamp()) {
//                        recorder.setTimestamp(time);
//                        recorder.record(slideFrame);
//                    }
//                    //}
//                    cvReleaseImage(iplImage);
//                    iplImage.release();
//                }
//                recorder.stop();
//                recorder.release();
//                addVideoToGallery(video_output, "CampaignSlideShow", context);
//            } catch (FFmpegFrameRecorder.Exception e) {
//                e.printStackTrace();
//                System.out.println(e);
//                Toast.makeText(context,"Cannot create the video. Please try again later.",Toast.LENGTH_SHORT);
//            }finally {
//                System.gc();
//                Pointer.deallocateReferences();
//            }
//        }
//    }

    /**
     * Combining the campaign videos and campaign slides all together in one video
     **/
    public void combineClipsAndVideos(){
        try{
//            String videoPath = Environment.getExternalStorageDirectory().getPath() + "/campaignVideos";
            String video_output = videoRoot.getAbsoluteFile().toString() + File.separator + "output.mp4";
//            File[] listOfVideoFiles= new File(videoPath.toString()).listFiles(); // You can provide SD Card path here.
//            if(listOfVideoFiles.length < 1)
//                return;

            ArrayList<VisibilityEntry> mFinalCampaignToBeDisplayed = new ArrayList<VisibilityEntry>();
            mFinalCampaignToBeDisplayed.addAll(mCampaignSlides);
            mFinalCampaignToBeDisplayed.addAll(mCampaignVideos);
            Collections.sort(mFinalCampaignToBeDisplayed,new SortBySlotNumber());

            String path = Environment.getExternalStorageDirectory().getPath() + "/campaignSlidesToDisplay"; // You can provide SD Card path here.
            File folder = new File(path);
            File[] listOfFiles = folder.listFiles();
            OpenCVFrameConverter.ToIplImage imgToFrame = new OpenCVFrameConverter.ToIplImage();
            int slideTime = SettingActivity.getSlideTime(mContext);
            Frame frame;
            FrameRecorder recorder = new FFmpegFrameRecorder(video_output, 640,480/*1270, 720*/,2);
            recorder.setVideoCodec(13);
            recorder.setFormat("mp4");
            //http://stackoverflow.com/questions/14125758/javacv-ffmpegframerecorder-properties-explanation-needed
            recorder.setFrameRate(25); // This is the frame rate for video. If you really want to have good video quality you need to provide large set of images.
            recorder.setPixelFormat(0); // PIX_FMT_YUV420P
            recorder.start();
            long startTime = System.currentTimeMillis();
            for(VisibilityEntry campaignData : mFinalCampaignToBeDisplayed){
                if(campaignData.getCampaignDataPath().contains(".mp4")){
                    FrameGrabber grabber = new FFmpegFrameGrabber(new File(campaignData.getCampaignDataPath()));
                    grabber.start();
                    while ((frame = grabber.grabFrame()) != null) {
                        long timeStamp = grabber.getTimestamp();
                        recorder.setTimestamp(timeStamp);
                        recorder.record(frame);
                    }
                    grabber.flush();
                    grabber.stop();
                }
                else if(campaignData.getCampaignDataPath().contains(".jpeg")){

                    opencv_core.IplImage iplImage = cvLoadImage(campaignData.getCampaignDataPath());
                    frame = imgToFrame.convert(iplImage);
                    long time = 12500L * (System.currentTimeMillis() - startTime);
                    if (time > recorder.getTimestamp()) {
                        recorder.setTimestamp(time);
                        recorder.record(frame);
                    }
                    cvReleaseImage(iplImage);
                    iplImage.release();
                }
            }
            recorder.stop();
            recorder.release();

//            List<FrameGrabber> fgs = new ArrayList<FrameGrabber>();
//            for(File f : listOfVideoFiles)
//            {
//                FrameGrabber grabber = new FFmpegFrameGrabber(f);
//                grabber.start();
//                fgs.add(grabber);
//            }
//            String path = Environment.getExternalStorageDirectory().getPath() + "/campaignSlidesToDisplay"; // You can provide SD Card path here.
//            File folder = new File(path);
//            File[] listOfFiles = folder.listFiles();
//            OpenCVFrameConverter.ToIplImage imgToFrame = new OpenCVFrameConverter.ToIplImage();
//
//            //slidesToVideo
//            int slideTime = SettingActivity.getSlideTime(mContext);
//            FrameRecorder recorder = new FFmpegFrameRecorder(video_output, 1270, 720,2);
//            recorder.setVideoCodec(13);
//            recorder.setFormat("mp4");
//            //http://stackoverflow.com/questions/14125758/javacv-ffmpegframerecorder-properties-explanation-needed
//            recorder.setFrameRate(25); // This is the frame rate for video. If you really want to have good video quality you need to provide large set of images.
//            recorder.setPixelFormat(0); // PIX_FMT_YUV420P
//            recorder.start();
//            Frame frame;
//            for(FrameGrabber fg : fgs)
//            {
//                while ((frame = fg.grabFrame()) != null) {
//                    recorder.record(frame);
//                }
//                fg.stop();
//            }
//            long startTime = System.currentTimeMillis();
//            for (int j = 0; j < listOfFiles.length; j++ ) {
//                opencv_core.IplImage iplImage = cvLoadImage(listOfFiles[j].getAbsolutePath());
//                Frame slideFrame = imgToFrame.convert(iplImage);
//                long time = 12500L * (System.currentTimeMillis() - startTime);
//                if (time > recorder.getTimestamp()) {
//                    recorder.setTimestamp(time);
//                    recorder.record(slideFrame);
//                }
//                cvReleaseImage(iplImage);
//                iplImage.release();
//            }
//            recorder.stop();
//            recorder.release();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  Creating a longer duration video
     *  after combining the campaign slides and campaign videos
     **/
    public void append_videos(Context context){
        String video_output = videoRoot.getAbsoluteFile().toString() + File.separator + "output.mp4";
        File output_video = new File(video_output);
        if(!output_video.exists())
            return;
        try{
            Movie output = new Movie();
            List<Track> videoTracks = new LinkedList<Track>();
            List<Track> audioTracks = new LinkedList<Track>();
            for (int i=0;i<12;i++) {
                Movie videoMovie = MovieCreator.build(output_video.getAbsolutePath());
                for (Track t : videoMovie.getTracks()) {
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                }
            }
            if (videoTracks.size() > 0) {
                output.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
            if (audioTracks.size() > 0) {
                output.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            Container out = new DefaultMp4Builder().build(output);
            FileChannel fc = new RandomAccessFile(String.format(videoRoot.getPath()+"/final.mp4"), "rw").getChannel();
            out.writeContainer(fc);
            fc.close();

            // adding to the application directory
            addVideoToGallery(video_output, "CAMPAIGN-VIDEO-FINAL", context);
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }

    /**
     * adding the campaign slides to the media-store //
     * updating the media store
     **/
    public void addVideoToGallery(final String filePath, final String fileName, /*final long fileSize,*/ final Context context) {
        File file = new File(filePath);
        if(!file.exists()){
            try{
                file.createNewFile();
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        Random r = new Random();
        String id = String.valueOf(r.nextInt(3000));
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media._ID,id);
        values.put(MediaStore.Video.Media.TITLE, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        values.put(MediaStore.Video.Media.SIZE, (new File(filePath)).getTotalSpace());
        values.put(MediaStore.Video.Media.DESCRIPTION, "");
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        createThumbnail(filePath,id);
    }

    /**
     * create a thumbnail for the given image
     **/
    public void createThumbnail(String filePath, String id){
        Bitmap videoThumb = ImageUtil.getThumbnailForVideo(filePath
                .toString());
        String videoSavePath = ImageUtil.getSaveVideoFilePath(
                filePath.toString(), ContentTree.VIDEO_PREFIX+id.toString());
        try {
            ImageUtil.saveBitmapWithFilePathSuffix(videoThumb,
                    videoSavePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * adding the campaign slides to the media-store //
     * updating the media-store
     **/
    public void deleteVideofromGallery(String filePath, final Context context) {
        File file = new File(filePath);
        if(file.exists()) {
            String[] projection = {MediaStore.Video.Media._ID};

            // Match on the file path
            String selection = MediaStore.Video.Media.DATA + " = ?";
            //		try {
            String[] selectionArgs = new String[]{filePath};
            // Query for the ID of the media matching the file path
            Uri queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            ContentResolver contentResolver = context.getContentResolver();
            Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
            if (c.moveToFirst()) {
                // We found the ID. Deleting the item via the content provider will also remove the file
                long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media._ID));
                Uri deleteUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                contentResolver.delete(deleteUri, null, null);
            } else {
                // File not found in media store DB
            }
            c.close();
            //		} catch (IOException e) {
            //
            //		}
        }
    }

    /**
     * adding the campaign slides to the media-store //
     * updating the media-store
     **/
    public void addImageToGallery(final String filePath, final String fileName, final long fileSize, final Context context) {
        File file = new File(filePath);
        if(!file.exists()){
            try {
                file.createNewFile();
            }
            catch (IOException e){
                e.printStackTrace();
            }

        }
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.DATA, filePath);
        values.put(MediaStore.Images.Media.SIZE, fileSize);
        values.put(MediaStore.Images.Media.DESCRIPTION, "");
        context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    /**
     * adding the campaign slides to the media-store //
     * updating the media-store
     **/
    public void deleteImagefromGallery(final File filePath, final Context context) {

        if(filePath.exists()) {
            String[] projection = {MediaStore.Images.Media._ID};

            // Match on the file path
            String selection = MediaStore.Images.Media.DATA + " = ?";
            try {
                String[] selectionArgs = new String[]{filePath.getCanonicalPath()};
                // Query for the ID of the media matching the file path
                Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = context.getContentResolver();
                Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
                if (c.moveToFirst()) {
                    // We found the ID. Deleting the item via the content provider will also remove the file
                    long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                    Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                    contentResolver.delete(deleteUri, null, null);
                } else {
                    // File not found in media store DB
                }
                c.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * fetching the campaign videos from database
     * preparing the campaign videos to be shown
     **/
    public void prepareCampaignVideos() {
        //TODO:fetch the campaign videos
        try {
            String intermediateVideoPath = Environment.getExternalStorageDirectory().getPath() + "/campaignVideos";
            //File[] listOfVideoFiles = new File(videoPath.toString()).listFiles(); // You can provide SD Card path here.
//            if (listOfVideoFiles.length < 1)
//                return;
            if (mCampaignVideos.size() < 1) {
                return;
            }
            File intermediateFolder = new File(intermediateVideoPath);
            CommonUtil.deleteRecursive(intermediateFolder);
            if(!intermediateFolder.exists()){
                intermediateFolder.mkdir();
            }
            //removing the audio
            //for (int i = 0; i < listOfVideoFiles.length; i++) {
            for (VisibilityEntry videoEntry : mCampaignVideos) {

                Movie videoMovie = MovieCreator.build(videoEntry.getCampaignDataPath());
                Movie output = new Movie();
                for (Track t : videoMovie.getTracks()) {

                    if (t.getHandler().equals("vide")) {
                        output.addTrack(new AppendTrack(t));
                    }

                    if (t.getHandler().equals("soun")) {
                        output.addTrack(new AppendTrack(new CroppedTrack(t, 1, 30)));
                    }
                }
                Container out = new DefaultMp4Builder().build(output);

                FileChannel fc = new RandomAccessFile(intermediateVideoPath + "/imtermediateVideo_" + videoEntry.getCampaignID() + ".mp4", "rw").getChannel();
                out.writeContainer(fc);
                fc.close();
                videoEntry.setCampaignDataPath(intermediateVideoPath + "/imtermediateVideo_" + videoEntry.getCampaignID() + ".mp4");
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    /**
     * fetching the campaign audios from database
     * preparing the campaign audios to be shown
     **/
    public void prepareCampaignAudios() {
        if(!audioRoot.exists()){
            audioRoot.mkdir();
        }
        //TODO:fetch the campaign audios
    }

    class SortBySlotNumber implements Comparator<VisibilityEntry>
    {
        // Used for sorting in ascending order of
        // slot number
        public int compare(VisibilityEntry a, VisibilityEntry b)
        {
            return a.getSlotNumber() - b.getSlotNumber();
        }
    }
}