package data.com.datacollector.model;

/**
 * Model class for PPG data
 * Created by ritu on 11/17/17.
 */

public class PPGData {
    private float heartRate;
    private String timestamp;

    public PPGData(float heartRate, String timestamp){
        this.heartRate  = heartRate;
        this.timestamp = timestamp;
    }

    public float getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
