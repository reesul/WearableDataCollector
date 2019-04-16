# Wearable Data Collector
This readme is developed to aid those contributing to its development. Functions and files are generally well commented, so this document serves mainly to describe the flow of the application. 

## Application Flow
The majority of this application relies on running two Services in the background: one for accessing sensors (accelerometer, gyroscope, PPG for heart rate) and one for accessing BLE scans. These two services are handled almost identically, though they are kept logically separate. These services also require a ServiceHandler that communicates between services and the main thread, as well as maintain the services as background operations. Event handling is done through DataCollectReceiver, an instance of a BroadcastReceiver. The two main events are intiating data saves (using an internal alarm) and initiating data transfer (when power is connected). The latter is used for transferring data via Wi-Fi (HTTP POST message) to a known server or via Bluetooth depending on how the app was configured at install.

More concretely, the basic flow of the app is as follows:
1. HomeActivity initializes the interface and a few error handling objects
   - If the user has not granted the necessary permissions yet, those will be requested
   - Initializing the interface includes setting up the list of activities that the user can scroll through to label their data. These labels can be changed in model.Const.DEFAULT_ACTIVITIES_LIST_TEXT
      - Tapping one of these entries simply adds the timestamp and activity to a file, 'activities.txt' under the folder of today's date
2. The user will start the service by tapping the button, which starts the BLE scanning Service and Sensor (accelerometer, gyroscope, heart rate sensors) Services
   - Services run on a background thread with a power state which allows the service to continue running with the screen off
   - A service handler is set up to take messages from another thread. This is used to restart the data collection services in case the OS kills the processes
   - An alarm is set to go off every 5 minutes so data can be saved to file
3. An alarm goes off every 5 minutes; this throws an Intent which is caught by DataCollectReceiver
   - This signals the services to save data to files (located in the app's storage under a folder named based on the current date)
4. When the watch is plugged in, it will attempt to transmit all its data to a server via Wifi (HTTP POST) or Bluetooth receiver, depending on how the Const. file is configured
   - Will send one folder (day of data) at a time; folder is put into .zip format
   - Once a day's files have been sent successfully, those files are deleted unless the folder name is today's date
   - For HTTP, the server's address is hardcoded, expected to be running on port 9000 of the host
   - For Bluetooth, the host name is hardcoded; the app will search for the host name's device within the set of devices the watch is paired with
   
   
## Notes
- The rate at which Android samples sensors is not as consistent as would be desired. The OS views the sampling period (set when registering sensor listeners) as a 'guideline'. The Const. file sets the sampling period to allow 25Hz sampling, but the real rate is between 18 and 23 Hz, depending on the watch's power state
- Sampling is done over a short period before data is released. Getting samples in real time is power-hungry
- The sample time reported by the OS is not real-time, so we normalize this based on an initialization timestamp that is set up when starting sensor service (Util.initTimeStamps)
