package com.zxt.dlna.service;

/**
 * Created by sjaltran on 6/1/17.
 */

public class Constants {
    public interface ACTION {
        public static String MAIN_ACTION = "com.zxt.dlna.service.MediaService.action.main";
        public static String STARTSLIDESHOW_ACTION = "com.zxt.dlna.service.MediaService.action.startslideshow";
        public static String STARTVIDEO_ACTION = "com.zxt.dlna.service.MediaService.action.startvideo";
        public static String STARTFOREGROUND_ACTION = "com.zxt.dlna.service.MediaService.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.zxt.dlna.service.MediaService.action.stopforeground";
    }
    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
    }
}
