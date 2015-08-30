# Audio-based-probing-of-the-environment
Audio-based probing of the environment (Android) Communication Networks 2 Project in Winter 2014-15 semester

**Group Members**  

1. Dibyojyoti Sanyal (https://github.com/Dibyojyoti)  
2. Anirban Chatterjee (https://github.com/anirban99)  
3. Pranay Sarkar (https://github.com/pranay22)  


**System Requirements**

At least three Android devices with version 4.1 or higher.

**Demo Video of the App**

https://www.youtube.com/watch?v=kQhqLmSzrVo

**Project Description**


This app does probing of environment based on Audio. To do the probing, at least 3 android devices are needed. In the app, WiFi-Direct is used to communicate between devices (nodes). One node can work as server at one point of time and rest of the noes can work as client. Client/Server mode can be chosen in the main screen.  
After the devices sucessfully connects to one another it srarts playing Gaussian noise and starts probing the environment. Recoded files are stored in lossless format (.flac) in the android devices.

**Steps to operate the android app**

*1st scenario: Connecting one client and one server.*

1. Open the app in two android devices.
2. Open Wifi Direct in both the devices 
3. Start the server in one device using the button 'Start server'
4. Start the client in another device using the button 'start client'
5. Client sends a request to connect WifiDirect to the server.
6. Accept the request from client device.
7. The sounds starts recording automatically.
8. Check the recorded sound file in th SD card.
9. Close the server connection using 'Stop Server'
10. Close the client connection using 'Stop Client'.


*2nd scenario: Connecting one client with two servers.*

1. Open the app in three android devices.
2. Open Wifi Direct in all the three devices 
3. Start the server in one device using the button 'Start server'
4. Start the client in another device using the button 'start client'
5. Client sends a request to connect WifiDirect to one server device.
6. Accept the request from client device.
7. The sounds starts recording automatically.
8. Meanwhile start the server in the third device as well.
9. After recording the sound in the first server device, the client connects to the second server device.
10. Client sends a request to connect WifiDirect to the second server device.
11. Accept the request from client device.
12. The sounds starts recording automatically.
13. Check the recorded sound file in the SD card of both the server devices.
14. Close all the server connection using 'Stop Server'
15. Close all the client connection using 'Stop Client'.


**More Details can be found on:**  
http://pranay22.github.io/Audio-based-probing-of-the-environment/
