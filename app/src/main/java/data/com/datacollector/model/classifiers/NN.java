package data.com.datacollector.model.classifiers;

import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
import java.util.List;

import data.com.datacollector.model.SensorData;

public class NN extends Classifier {
    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel = null;

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite = null;

    public void setModel(String path,String fileName){

    }

    public double[] predict(List<SensorData> accWindowData, List<SensorData> gyroWindowData){
        double [] prob = {};
        return prob;
    }

    //TODO: Make sure this is called when stopping the predictions
    public void close() {
        if (tflite != null)
            tflite.close();
        tflite = null;
        tfliteModel = null;
    }
}
