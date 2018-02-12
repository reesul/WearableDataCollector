# WearableDataCollector
Android wear app for collecting BLE device scans and watch sensor data (e.g. IMU, PPG)


App has been tested on Polar M600 watches. Moto 360 watches have issues with BLE scans, and the app will crash shortly after opening

Data is sent to a server (IP address hardcoded, viewable in data.com.datacollector.model.Const) whenever the watch is plugged in. The watch must be connected to the same WiFi network as the server. If a phone is paired to the watch, then the connection to the server may timeout. Turning off Bluetooth on the connected phone for a moment will fix this issue.

Send me a message if you would like the server code



TODO items:
    Setup persistent server so watches can transfer files from any Wi-Fi network
    Add file streaming through OkHttp to lessen memory requirements of the app