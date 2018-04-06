package data.com.datacollector.model;

import java.util.List;

/**
 * Model class for the management of the list of activities. Created in case future development
 * requieres to add a remote server for loading the list of activities to be used
 * Created by ROGER on 2/5/2018.
 */

public class ActivitiesList {

    /**
     * Configuration variables for the obtention of the activities list
     */
    public enum ActivitiesSource {
        DEFAULT, REMOTE
    }

    public ActivitiesList(){
    }

    public String[] getList(){
        if (Const.ACTIVITIES_LIST_SOURCE == ActivitiesSource.REMOTE){
            /*
                //Future feature: Get activities list from a REST api. Not required so far
                TODO: Create code for access to remote activity list. Might need asynchronous management
                return remoteList;
            */
        }
        //The default list will always be shown in case the before options fail to load
        return Const.DEFAULT_ACTIVITIES_LIST_TEXT;
    }

}