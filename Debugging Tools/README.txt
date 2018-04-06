If the app fails, there are some steps that we can follow to debug it even if the wearable is not
connected to Android Studio. In other words, we can write the logs into files following the instructions:

1.- Copy the file MyApplication.java into the project. Could be under the views folder
2.- In the manifest, change the application name to be: android:name=".view.MyApplication"
3.- Add the following permission to the manifest 
    <uses-permission android:name="android.permission.READ_LOGS" />
4.- Files are saved under the logs folder in the DC data directory


There might also be some exception that is not being caught somewhere in the code. 
The CuztomizedExceptionHandler handles any uncaught exception

1.- Copy the file CuztomizedExceptionHandler.java into the project. Could be under the views folder
2.- In the main activity, copy and paste the following under the onCreate method (first)

        //Custom uncaugh exception handling
        Thread.setDefaultUncaughtExceptionHandler(new CustomizedExceptionHandler(this.getFilesDir().toString()));
        //Finish custom
3.- Files are saved under the logs folder in the DC data directory
