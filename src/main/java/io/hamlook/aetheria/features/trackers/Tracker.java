package io.hamlook.aetheria.features.trackers;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.utils.GsonManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;

public class Tracker {

    public String trackerID,trackerName;
    /**
     * Make sure the Keys in both of the below HashMaps & Value in the third HashMap are the SAME
     */
    public TrackedData data;

    public Tracker(String trackerID,String trackerName){
        this.trackerID=trackerID;
        this.trackerName=trackerName;
        data = new TrackedData(new HashMap<>(),new HashMap<>());
    }

    public void load(){
        File file = getFile();
        try{
            if(!file.exists()){
                file.createNewFile();
                return;
            }
            TrackedData data = GsonManager.GSON.fromJson(new FileReader(file),TrackedData.class);
            if(data == null){
                Aetheria.logger.warning("[Trackers] Data for " + trackerName + " is corrupted.");
                file.renameTo(getFileCorrupted());
                save();
            }
        }catch (Exception ex){
            Aetheria.logger.severe("[Trackers] Could not Load data of " + trackerID + " of tracker: " + trackerName);
            Aetheria.logger.severe(ex.getMessage());
        }
    }

    public void save(){
        File file = getFile();
        try{
            if(!file.exists()){
                file.createNewFile();
                return;
            }
            if(this.data.isEmpty()){
                Aetheria.logger.warning("[Trackers] Data for " + trackerName + " is empty.");
            }
            FileWriter writer = new FileWriter(file);
            writer.write(GsonManager.GSON.toJson(this.data));
            writer.close();
        }catch (Exception ex){
            Aetheria.logger.severe("[Trackers] Could not Save data of " + trackerID + " of tracker: " + trackerName);
            Aetheria.logger.severe(ex.getMessage());
        }
    }

    public File getFile(){
        return  new File(TrackerManager.trackerFolder,trackerID+".tracked");
    }

    public File getFileCorrupted(){
        return  new File(TrackerManager.trackerFolder,trackerID+".tracked.corrupted");
    }

    public void addLine(String key,String value,Object baseValue){
        data.add(key,value,baseValue);
    }
    public void addLine(String key,String value){
        addLine(key,value,null);
    }

    public boolean updateValue(String key,Object newVal){
        if(data.has(key)){
            return data.update(key,newVal);
        }
        return false;
    }

}
