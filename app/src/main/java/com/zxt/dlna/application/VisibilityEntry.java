package com.zxt.dlna.application;

import android.os.Environment;

import java.io.File;
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
    private int _id;
    private String _dataPath;
    private int _campaignType;//1-Campaign 2-LocalProduct 3-MyStore 4-Video
    private int _slotNumber; //selected slot number
    private int _nSlots; //Number of slots required for video
    private Date _startDateTime;//can be null
    private Date _endDateTime;//can be null
    private Integer _price;//price in paisa, can be null
    private Integer _discount;//Discount in paisa, can be null
    private String _productName;//can be null
    private String _offerMessage;//can be null

    // Empty constructor
    public VisibilityEntry(){};
    //constructor
    public VisibilityEntry(String dataPath, int campaignType){
        this._dataPath = dataPath;
        this._campaignType = campaignType;
        this._startDateTime = null;
        this._endDateTime = null;
        this._price = null;
        this._discount = null;
        this._productName = null;
        this._offerMessage = null;
        this._slotNumber = 0;
        this._nSlots =0;
    }
    //constructor
    public VisibilityEntry(String dataPath, int campaignType, int slot, int numberofSlots){
        this._dataPath = dataPath;
        this._campaignType = campaignType;
        this._startDateTime = null;
        this._endDateTime = null;
        this._price = null;
        this._discount = null;
        this._productName = null;
        this._offerMessage = null;
        this._slotNumber = slot;
        this._nSlots = numberofSlots;
    }
    //constructor
    public VisibilityEntry(String dataPath, int campaignType, int id){
        this._id = id;
        this._dataPath = dataPath;
        this._campaignType = campaignType;
        this._startDateTime = null;
        this._endDateTime = null;
        this._price = null;
        this._discount = null;
        this._productName = null;
        this._offerMessage = null;
        this._slotNumber = 0;
        this._nSlots =0;
    }
    //setting imagePath
    public void setCampaignDataPath(String dataPath) {
        this._dataPath = dataPath;
    }
    //setting campaign type
    public void setCampaignType(int campaignType) {
        this._campaignType = campaignType;
    }
    //setting the slot
    public void setSlotNumber(int slotNumber) {
        this._slotNumber = slotNumber;
    }
    //setting number of slots occupied by the video
    public void setNumberofSlots(int numberOfSlots) {
        this._nSlots = numberOfSlots;
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

    public void setcampaignID(int id){this._id = id;}

    //geting imageID
    public int getCampaignID(){return this._id;}
    //getting imagePath
    public String getCampaignDataPath() {
        return this._dataPath;
    }
    //getting campaign type
    public int getCampaignType() {
        return this._campaignType;
    }
    //setting the slot
    public int getSlotNumber() {
        return this._slotNumber;
    }
    //setting number of slots occupied by the video
    public int getNumberOfSlots() {
        return this._nSlots;
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

    public List<VisibilityEntry> getAllCampaignData(){
        List<VisibilityEntry> allCampaignData =new ArrayList<VisibilityEntry>();
        //TODO: write a function to fetch images from Snapbilling database
        //remove when importing from the Snapbilling application
        //remove start
        Random r = new Random();
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + "campaignData");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //fetching the images from the local storage
        //replace below with the logic to fetch from the snapbilling application
        if (folder.listFiles().length > 0) {
            int i=0;
            for (File file : folder.listFiles()) {
                VisibilityEntry campaignData = new VisibilityEntry();
                campaignData.setcampaignID(r.nextInt(1000));
                campaignData.setSlotNumber(r.nextInt(41));
                try {
                    campaignData.setCampaignDataPath(file.getCanonicalPath());
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                if(file.getName().endsWith(".mp4")){
                    campaignData.setCampaignType(4);
                }
                else {
                    if (i < 5) {
                        campaignData.setCampaignType(1);

                    } else if (i == 39) {
                        campaignData.setCampaignType(3);
                    } else {
                        campaignData.setCampaignType(2);
                    }
                }
                i++;
                campaignData.setProductName(file.getName());
                // Adding each image to campaignImagesList
                allCampaignData.add(campaignData);
            }
        }
        //remove end
        return allCampaignData;
    }
}

