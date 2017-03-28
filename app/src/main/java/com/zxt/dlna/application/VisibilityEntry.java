package com.zxt.dlna.application;

import android.os.Environment;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created by sjaltran on 14/12/16.
 */

public class VisibilityEntry {
    //class members
    public int _id;
    public String _imagepath;
    public int _campaignType;//1-Campaign 2-LocalProduct 3-MyStore
    public Date _startDateTime;//can be null
    public Date _endDateTime;//can be null
    public Integer _price;//price in paisa, can be null
    public Integer _discount;//Discount in paisa, can be null
    public String _productName;//can be null
    public String _offerMessage;//can be null

    // Empty constructor
    public VisibilityEntry(){};
    //constructor
    public VisibilityEntry(String imagePath, int campaignType){
        this._imagepath = imagePath;
        this._campaignType = campaignType;
        this._startDateTime = null;
        this._endDateTime = null;
        this._price = null;
        this._discount = null;
        this._productName = null;
        this._offerMessage = null;
    }
    //constructor
    public VisibilityEntry(String imagePath, int campaignType, int id){
        this._id = id;
        this._imagepath = imagePath;
        this._campaignType = campaignType;
        this._startDateTime = null;
        this._endDateTime = null;
        this._price = null;
        this._discount = null;
        this._productName = null;
        this._offerMessage = null;
    }

    //setting imagePath
    public void setImagePath(String imagePath) {
        this._imagepath = imagePath;
    }
    //setting campaign type
    public void setCampaignType(int campaignType) {
        this._campaignType = campaignType;
    }
    //setting startDateTime
    public void setStartDateTime(Date startDateTime) {
        this._startDateTime = startDateTime;
    }
    //setting endDateTime
    public void setEndDateTime(Date endDateTime) {
        this._endDateTime = endDateTime;
    }
    //setting price in paisa
    public void setPrice(Integer price) {
        this._price = price;
    }
    //setting discount in paisa
    public void setDiscount(Integer discount) {
        this._discount = discount;
    }
    //setting productName
    public void setProductName(String productName) {
        this._productName = productName;
    }
    //setting offerMessage
    public void setOfferMessage(String offerMessage) {
        this._offerMessage = offerMessage;
    }

    public void setImageID(int id){this._id = id;}

    //geting imageID
    public int getImageID(){return this._id;}
    //getting imagePath
    public String getImagePath() {
        return this._imagepath;
    }
    //getting campaign type
    public int getCampaignType() {
        return this._campaignType;
    }
    //getting startDateTime
    public Date getStartDateTime() {
        return this._startDateTime;
    }
    //getting endDateTime
    public Date getEndDateTime() {
        return this._endDateTime;
    }
    //getting price in paisa
    public Integer getPrice() {
        return this._price;
    }
    //getting discount in paisa
    public Integer getDiscount() {
        return this._discount;
    }
    //getting productName
    public String getProductName() {
        return this._productName ;
    }
    //getting offerMessage
    public String getOfferMessage() {
        return this._offerMessage;
    }

    public List<VisibilityEntry> getAllCampaignImages(){
        List<VisibilityEntry> allCampaignImages =new ArrayList<VisibilityEntry>();

        //TODO: write a function to fetch images from Snapbilling database
        //remove when importing from the Snapbilling application
        //remove start
        Random r = new Random();
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "campaignImages");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //fetching the images from the local storage
        //replace below with the logic to fetch fromt the snapbilling application
        if (folder.listFiles().length > 0) {
            int i=0;
            for (File file : folder.listFiles()) {
                VisibilityEntry campaignData = new VisibilityEntry();
                campaignData.setImageID((int) r.nextInt(1000));
                try {
                    campaignData.setImagePath(file.getCanonicalPath());
                }
                catch (IOException e){

                }
                if(i< 39) {
                    campaignData.setCampaignType(1);
                }else if(i==39){
                    campaignData.setCampaignType(3);
                }else{
                    campaignData.setCampaignType(2);
                }
                i++;
                campaignData.setProductName(file.getName());
                // Adding each image to campaignImagesList
                allCampaignImages.add(campaignData);
            }
        }
        //remove end
        return allCampaignImages;
    }

    public int getNumberOfSlides(List<VisibilityEntry>allImages){
        int slideCount = 0;
        //logic to count the number of slides
        return slideCount;
    }

    /**
     * Class to filter files which are having image file extension
     * */
    class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return (name.endsWith(".png") || name.endsWith(".jpg"));
        }
    }
}
