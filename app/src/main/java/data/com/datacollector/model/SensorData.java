package data.com.datacollector.model;

/**
 * model class for Accelerometer and Gyroscope data
 * Created by ritu on 10/29/17.
 */

public class SensorData {
    private float x;
    private float y;
    private float z;
    private String timestamp;
    public SensorData(float x, float y, float z, String timestamp){
        this.x = x;
        this.y = y;
        this.z = z;
        this.timestamp = timestamp;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
