package data.com.datacollector.model;

import data.com.datacollector.utility.Util;

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

    public String[] getListText(){
        if (Const.ACTIVITIES_LIST_SOURCE == ActivitiesSource.REMOTE){
            /*
                TODO: Create code for access to remote activity list. Might need asynchronous management
                return remoteList;
            */
        }
        //The default list will always be shown in case the before options fail to load
        return Const.CUSTOM_ACTIVITIES_LIST;
    }

    public String[] getListTag(){
        if (Const.ACTIVITIES_LIST_SOURCE == ActivitiesSource.REMOTE){
            /*
                TODO: Create code for access to remote activity list. Might need asynchronous management
                return remoteList;
            */
        }
        //The default list will always be shown in case the before options fail to load
        return Util.removeSpaces(Const.CUSTOM_ACTIVITIES_LIST);
    }

}
