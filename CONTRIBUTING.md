# Wearable Data Collector
This readme is developed to aid those contributing to its development. Functions and files are generally well commented, so this document serves mainly to describe the flow of the application. 

## Application Flow
The majority of this application relies on running two Services in the background: one for accessing sensors (accelerometer, gyroscope, PPG for heart rate) and one for accessing BLE scans. These two services are handled almost identically, though they are kept logically separate. These services also require a ServiceHandler that communicates between services and the main thread, as well as maintain the services as background operations. Event handling is done through DataCollectReceiver, an instance of a BroadcastReceiver. The two main events are intiating data saves (using an internal alarm) and initiating data transfer (when power is connected). The latter is used for transferring data via Wi-Fi (HTTP POST message) to a known server or via Bluetooth depending on how the app was configured at install.

More concretely, the basic flow of the app is as follows:
1. HomeActivity initializes the interface and a few error handling objects
   - If the user has not granted the necessary permissions yet, those will be requested
   - Initializing the interface includes setting up the list of activities that the user can scroll through to label their data. These labels can be changed in model.Const.DEFAULT_ACTIVITIES_LIST_TEXT
     - Tapping one of these entries simply adds the timestamp and activity to a file, 'activities.txt' under the folder of today's date
2. The user will start the service by tapping the button, 
