package data.com.datacollector.model;

/**
 * Every model should extend THIS base model and MUST implement the toString method
 */
public abstract class BaseSensorDataModel {

    private String timestamp = "";

    public BaseSensorDataModel(){
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * This method should return the formatted string to be saved from the instance attrs. This is
     * how each row will look like in the files
     * For example: timestamp,var1,var2 as a string
     * @return The string to be saved in file
     */
    public abstract String toString();
}
