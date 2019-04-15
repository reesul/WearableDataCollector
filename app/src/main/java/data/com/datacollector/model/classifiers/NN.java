package data.com.datacollector.model.classifiers;

import org.tensorflow.lite.Interpreter;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    protected Interpreter tflite = null;

    public NN(String path){
        this.path = path;
    }

    public void setModel() throws IOException{
        tfliteModel = loadModelFile();
        tflite = new Interpreter(tfliteModel, tfliteOptions);
    }

    public void setLabels(String []labels){
        labelList = Arrays.asList(labels);
    }

    //TODO. Make sure this works
    public double[] predict(List<SensorData> accWindowData, List<SensorData> gyroWindowData){
        double [][] prob = null;
        ByteBuffer data = null;

        tflite.run(data, prob);

        return prob[0];
    }

    //TODO: Make sure this is called when stopping the predictions
    public void close() {
        if (tflite != null)
            tflite.close();
        tflite = null;
        tfliteModel = null;
    }

    private MappedByteBuffer loadModelFile() throws IOException {

        FileInputStream inputStream = new FileInputStream(path);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
