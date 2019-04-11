# WearableDataCollector
Wear OS (Android) app for collecting BLE device scans and watch sensor data (e.g. IMU, PPG, Bluetooth Low Energy)

This application has been built for the Polar M600 Android Smart watch, specifically Wear OS 2.0 (Android 8.0). Older versions of Android handle background services (which are necessary for long-term data collection) differently, so it is unlikely the application will function properly on anything newer than Android 8.0, regardless of the brand and model.

## Installation
To run this app, download Android Studio as is. Once installed, you will likely need to install Android SDK Build-Tools 28.0.3 to build the app for Android 8.0 systems. 

Installing the application to the watch takes a few extra steps before the APK can be installed via USB cable. 
1. The watch must be on Android 8.0 (Check Settings -> System -> About -> Version)
2. Enable Developer Options: Go to Settings -> System -> About, and tap "Build Number" until a message, "You are now a developer!" displays.
3. Enable ADB debugging: Go to Settings -> Developer options, and make sure ADB debugging is on.
4. Connect the watch to the computer with Android Studio. A dialog message should show asking for permission to debug; grant permission. 

Running the app through Android studio will not load the data collection app onto the watch. The screen will ask for two types of permission (access to sensors, access to location i.e. Bluetooth). Once these are granted, the watch is ready for data collection.

## Usage
This application features a simple interface for selecting an activity label and starting/stopping data collection. The three images below show the watch face with data service off (left) and data service on (middle) and just after a label entry has been pressed (right)

![alt text](images/watch-app-basic.png), ![alt text](images/watch-app-running.png)

Data is sent to a server (IP address hardcoded, viewable in data.com.datacollector.model.Const) whenever the watch is plugged in. The watch must be connected to the same WiFi network as the server. If a phone is paired to the watch, then the connection to the server may timeout. Turning off Bluetooth on the connected phone for a moment will fix this issue.

Send me a message if you would like the server code



TODO items:
    Setup persistent server so watches can transfer files from any Wi-Fi network
    Add file streaming through OkHttp to lessen memory requirements of the app
