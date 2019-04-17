package data.com.datacollector.model.classifiers;

import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import data.com.datacollector.model.SensorData;

import static data.com.datacollector.model.Const.DIET_MODEL_LABELS;

public class NN extends Classifier {
    /** The loaded TensorFlow Lite model. */
    private MappedByteBuffer tfliteModel = null;
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    private String path = "";
    private List<String> labelList;
    private final String TAG = "NN";

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite = null;

    public NN(String path){
        this.path = path;
    }

    public void setModel() throws IOException{
        Log.d(TAG, "setModel: About to set model");
        tfliteModel = loadModelFile();

        Log.d(TAG, "setModel: About to set interpreter");
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        Log.d(TAG, "setModel: Interpreter is set");
    }

    public void setLabels(String []labels){
        //TODO: Consider uploading this from a file
        labelList = Arrays.asList(labels);
    }

    public float[] predict(List<SensorData> accWindowData, List<SensorData> gyroWindowData){
        Log.d(TAG, "predict: About to make prediction");
        float [][] prob = new float[1][labelList.size()];
        int floatByteSize = 4;
        int windowSize = accWindowData.size();
        int totalChannels = 6;

        ByteBuffer data = ByteBuffer.allocateDirect(floatByteSize*windowSize*totalChannels);//Allocate memory based on the array's dim
        data.order(ByteOrder.nativeOrder());
        data.rewind();

        for (int i=0; i<accWindowData.size();i++){
            data.putFloat(gyroWindowData.get(i).getX());
            data.putFloat(gyroWindowData.get(i).getY());
            data.putFloat(gyroWindowData.get(i).getZ());
            data.putFloat(accWindowData.get(i).getX());
            data.putFloat(accWindowData.get(i).getY());
            data.putFloat(accWindowData.get(i).getZ());
        }

        tflite.run(data, prob);

        return prob[0];
    }

    public void close() {
        Log.d(TAG, "close: Closing");
        if (tflite != null)
            tflite.close();
        tflite = null;
        tfliteModel = null;
    }

    private MappedByteBuffer loadModelFile() throws IOException {

        Log.d(TAG, "loadModelFile: About to load model at " + path);
        FileInputStream inputStream = new FileInputStream(path);
        Log.d(TAG, "loadModelFile: about to get channel");
        FileChannel fileChannel = inputStream.getChannel();
        Log.d(TAG, "loadModelFile: About to map");
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
