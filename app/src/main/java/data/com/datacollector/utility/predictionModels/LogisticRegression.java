package data.com.datacollector.utility.predictionModels;

import android.util.Log;

import org.apache.commons.math3.stat.descriptive.moment.Mean;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;

import data.com.datacollector.model.SensorData;

public class LogisticRegression {
    public String TAG = "LogisticRegression";
    public int FEATURES_NUM;
    public int LABELS_NUM;
    public double[][] coefficients = null;
    public String[] labels = null;

    public LogisticRegression(int featuresNum, int labelsNum){
        FEATURES_NUM = featuresNum;
        LABELS_NUM = labelsNum;
    }

    /**
     * Sets the model coefficients from the file
     * @param path The path to the file with the model's coefficients
     */
    public void setCoefficients(String path, String name) throws Exception{
        Log.d(TAG, "setCoefficients: ");
        //FEATURES_NUM+1 because of the intercept values
        //coefficients = new double[LABELS_NUM][FEATURES_NUM+1];
        //NOTE: Last column is the intercept
//        coefficients = new double[][]{
//                {-1.17418086e+00,7.02809537e-01,-2.67468094e-01,-7.50706449},
//                {-5.76990074e-01,7.24835210e-01,6.22647498e-01,-6.08723637},
//                {2.98018067e+01,-2.10628869e+01,8.37464037e+00,-194.59924692},
//                {-3.05705060e+00,-2.99631219e+00,2.50150344e+00,-39.22753112},
//                {2.27818766e-02,-1.42197190e-01,4.09716518e+00,-37.17181864},
//                {1.55940224e+00,6.96031170e-01,8.85622009e-01,-16.01001668},
//                {6.66033551e+00,-8.14464677e+00,1.26597296e+01,-141.62667441},
//                {-1.70265270e+00,5.81349738e+00,-1.49216920e+00,-24.61572201},
//                {5.45166250e-01,3.04665292e-01,-2.81830489e+00,-17.76954197}
//        };

        double [][]coeff = new double[LABELS_NUM][FEATURES_NUM+1];

        File file = new File(path, name);
        int labelsNum = 0;
        int currentToken = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                StringTokenizer tokens = new StringTokenizer(line, ",");
                if(tokens.countTokens() < (FEATURES_NUM+1)){
                    throw new Exception("The number of feature coefficients does not match. Expected "  + String.valueOf(FEATURES_NUM) + ", found " + String.valueOf(tokens.countTokens()) + ". Make sure the intercept is added as the last column");
                }else{
                    currentToken = 0;
                    while(tokens.hasMoreTokens()){
                        coeff[labelsNum][currentToken] = Double.valueOf(tokens.nextToken());
                        currentToken++;
                    }
                    labelsNum++;
                }
            }
            br.close();
            if(labelsNum < LABELS_NUM){
                throw new Exception("The number of labels does not match. Expected " + String.valueOf(LABELS_NUM) + ", found " + String.valueOf(labelsNum));
            }
            coefficients = coeff;
        }
        catch (IOException e) {
            Log.d(TAG, "setCoefficients: No file found. Predictions cannot be made");
            throw e;
        }

    }

    public void setLabels(String labels[]){
        this.labels = labels;
    }

    public double [] predict(double features[]) throws Exception{
        Log.d(TAG, "predict: ");
        if (coefficients == null){
            throw new Exception("Coefficients must be set before predictions are made");
        }

        if (labels == null){
            throw new Exception("Labels must be set before predictions are made");
        }

        double []probabilities = new double[LABELS_NUM];
        double probabilitiesTotal = 0;//For normalization
        double p = 0;
        //double higherProb = 0;
        //int higherLblId = 0;

        for (int lblId = 0; lblId<LABELS_NUM; lblId++){
            //Checking the prob of every label
            p = getProbability(features, lblId);//Probability of this features belonging to label lblId
            probabilities[lblId] = p;
            probabilitiesTotal+=p;
        }

        //Normalization. Needed for the threshold algorithm for feedback request. So we can set up a threshold and more logic HERE
        //TODO: Feedback algorithm logic might be added here while verifying the probabilities of each class
        for (int lblId = 0; lblId<LABELS_NUM; lblId++){
            p = probabilities[lblId]/probabilitiesTotal;//Normalization
            probabilities[lblId] = p;
            /*if(p>higherProb){
                higherProb = p;
                // The id of the label with the highest probability.
                // Validations can be made here for feedback (Threshold, comparing against  others)
                // For threshold its simply verify the probability is above a threshold
                // While comparing, verify that there is no other probability from the other labels with a close value.
                // If so, it might be used for the labeling suggestion on feedback?
                //higherLblId = lblId;
            }*/
        }

        //return labels[higherLblId];
        return probabilities;
    }

    double sigmoid(double x){
        return 1.0/(1.0+Math.exp(-x));
    }

    /**
     * The probability that the features are from the label
     * @param features The features to evaluate
     * @param labelId The label to evaluate
     * @return The probability
     */
    double getProbability(double []features, int labelId){

        double dotProdVal = 0;

        for (int featurei=0; featurei<features.length; featurei++){
            dotProdVal += features[featurei]*coefficients[labelId][featurei];
        }

        //Since the columns in coefficients is FEATURES_NUM+1 and the last column is the intercept, FEATURES_NUM would get the last column
        return sigmoid(dotProdVal + coefficients[labelId][FEATURES_NUM]);
    }

    /**
     * This method calculates and returns the required features from an array
     * @return
     */
    public double[] getFeatures(List<SensorData> accData){
        Log.d(TAG, "getFeatures: ");
        //TODO: If number of features changes, this should be considered here and the features must be concatenated in a single feature array
//        double features[] = new double[FEATURES_NUM];
        double meanFeatures[] = getFeatureMean(accData);
        return meanFeatures;
    }
    //Returns the mean values on each axis
    public double[] getFeatureMean(List<SensorData> accData){
        Log.d(TAG, "getFeatureMean: ");

        double accx[] = new double[accData.size()];
        double accy[] = new double[accData.size()];
        double accz[] = new double[accData.size()];

        for (int i=0; i<accData.size(); i++){
            accx[i] =  accData.get(i).getX();
            accy[i] =  accData.get(i).getY();
            accz[i] =  accData.get(i).getZ();
        }

        Mean m = new Mean();

        double []f = {m.evaluate(accx), m.evaluate(accy), m.evaluate(accz)};
        return f;
    }

    //Returns the minimum values on each axis
    public double[] getFeatureMin(List<SensorData> accData){
        Log.d(TAG, "getFeatureMin: ");
        double minX = 1000, minY = 1000, minZ = 1000;
        double valX, valY, valZ;
        for (int i = 0; i<accData.size(); i++){
            //Get the minimum on each axis
            valX = accData.get(i).getX();
            valY = accData.get(i).getY();
            valZ = accData.get(i).getZ();
            if(valX<minX){
                minX = valX;
            }

            if(valY<minY){
                minY = valY;
            }

            if(valZ<minZ){
                minZ = valZ;
            }
        }
        double []f = {minX, minY, minZ};
        return f;
    }

    public int getFEATURES_NUM() {
        return FEATURES_NUM;
    }

    public void setFEATURES_NUM(int FEATURES_NUM) {
        this.FEATURES_NUM = FEATURES_NUM;
    }

    public int getLABELS_NUM() {
        return LABELS_NUM;
    }

    public void setLABELS_NUM(int LABELS_NUM) {
        this.LABELS_NUM = LABELS_NUM;
    }
}
