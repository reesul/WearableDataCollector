This file serves to show the expected output format of each file, those being accelerometer.txt, ble_data.txt, gyroscope_data.txt, and ppg_data.txt. The first line of every file is the serial number of the watch to help confirm which subject the data belongs to in case of any mixups. 


### Accelerometer and Gyrsocope
The formats for these files are identical, and each entry is comma separated. The first is the timestamp including date and time down to milliseconds. The next three numbers are floating points to represent the X, Y, and Z axes of the signal.

##### Example
11/03/18 23:58:00.175,-4.132405,5.4412785,-5.7523456
11/03/18 23:58:00.222,-3.7543387,4.5726843,-3.5581272
11/03/18 23:58:00.270,-3.3523445,7.0301127,-12.622139
11/03/18 23:58:00.318,-3.1106694,6.3768725,-4.302295


### Heart rate (PPG)
This format is simpler than ACC and GYRO; it is just the timestamp and corresponding heart rate

##### Example
11/03/18 23:58:02.344,79.0
11/03/18 23:58:06.431,80.0
11/03/18 23:58:07.431,81.0


### Bluetooth Low Energy Scans
This format is the least straightforward as it was changed to reduce the amount of memory required, as being in a public area would mean many devices are around at a single time. To compress this, only new timestamps and RSSI values are repeated, with the device name and raw data packet kept constant. 

Each field is between a pair of braces to help searching, and separate instances within those braces separated by a semicolon. The fields are comma separated. Several matlab files exist to reformat a ble_data.txt into a time series format. View those under [Matlab directory](matlab/)

##### Example
time(s):{11/03/18 23:58:00;11/03/18 23:58:05;11/03/18 23:58:10;11/03/18 23:58:15;11/03/18 23:58:20;11/03/18 23:58:25;11/03/18 23:58:30;11/03/18 23:58:35;11/03/18 23:58:36;11/03/18 23:58:45;11/03/18 23:58:50;11/03/18 23:59:00;11/03/18 23:59:05;11/03/18 23:59:10;11/03/18 23:59:15;11/03/18 23:59:16;11/03/18 23:59:25;11/03/18 23:59:30;11/03/18 23:59:35;11/03/18 23:59:40;11/03/18 23:59:45;11/03/18 23:59:50;11/03/18 23:59:55;11/04/18 00:00:00;11/04/18 00:00:05;11/04/18 00:00:10;11/04/18 00:00:15;11/04/18 00:00:20;11/04/18 00:00:25;11/04/18 00:00:30;11/04/18 00:00:35;11/04/18 00:00:40;11/04/18 00:00:45;11/04/18 00:00:50;11/04/18 00:00:55;11/04/18 00:01:00;11/04/18 00:01:05;11/04/18 00:01:10;11/04/18 00:01:15;11/04/18 00:01:20;11/04/18 00:01:30;11/04/18 00:01:35;11/04/18 00:01:40;11/04/18 00:01:45;11/04/18 00:01:50;11/04/18 00:01:55;11/04/18 00:02:00;11/04/18 00:02:05;11/04/18 00:02:10;11/04/18 00:02:15;11/04/18 00:02:20},mac:{6F:0B:83:00:0C:FC},name:{null},rssi:{-70;-70;-70;-74;-71;-71;-76;-85;-84;-76;-80;-68;-77;-76;-75;-71;-76;-69;-74;-92;-70;-70;-76;-79;-74;-76;-78;-74;-77;-76;-73;-78;-74;-73;-79;-73;-71;-91;-69;-70;-68;-71;-68;-73;-72;-74;-65;-66;-74;-74;-73},raw_data:{02011A0AFF4C001005031C4F8AB7}
time(s):{11/03/18 23:58:10;11/03/18 23:58:25;11/03/18 23:58:40;11/03/18 23:59:10;11/03/18 23:59:25;11/04/18 00:00:15;11/04/18 00:00:30;11/04/18 00:00:45;11/04/18 00:01:00;11/04/18 00:01:15;11/04/18 00:01:30;11/04/18 00:01:55},mac:{14:3A:2A:46:AC:03},name:{null},rssi:{-100;-99;-100;-99;-97;-99;-99;-99;-100;-99;-100;-98},raw_data:{1EFF06000109200210336C27504F46E125295E4CB7DF191F6170CE6D45D44F}
time(s):{11/03/18 23:58:25;11/03/18 23:59:30;11/03/18 23:59:45;11/04/18 00:01:45;11/04/18 00:02:00;11/04/18 00:02:05},mac:{31:F0:A3:EA:F3:06},name:{null},rssi:{-97;-99;-100;-101;-96;-97},raw_data:{1EFF060001092002A9B1C079FAA8B2F4ED559EA5C57668E3BDDE5BD2064C19}
