package io.hamlook.aetheria.features.trackers;

import io.hamlook.aetheria.core.ATHRConfig;

import java.io.File;
import java.util.HashMap;

public class TrackerManager {

    public static HashMap<String, Tracker> trackers = new HashMap<>();
    public static File trackerFolder = new File(ATHRConfig.configDirectory,"trackers");

    public static void initialise(){
        if(!trackerFolder.exists()){
            trackerFolder.mkdirs();
        }
        /**
         * Register Trackers here
         */
        loadAllTrackers();
    }

    public void register(Tracker tracker){
        trackers.put(tracker.trackerID, tracker);
    }

    public static void loadAllTrackers(){
        if(trackers.isEmpty()) return;
        trackers.values().forEach(Tracker::load);
    }

    //TODO: Call this when trackers need to be saved
    public static void saveAllTrackers(){
        if(trackers.isEmpty()) return;
        trackers.values().forEach(Tracker::save);
    }
}
