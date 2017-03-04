Timelapse - Sony Camera
=======================

Want to [Donate](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=LFFFQZD9UNKRC&lc=FR&item_name=Thibaud%20Michel&item_number=1&currency_code=EUR&bn=PP-DonationsBF%3abtn_donate_LG%2egif%3aNonHosted)?  

Project type: **Android**  
Current version: **2.1.0**  
Google Play store link: https://play.google.com/store/apps/details?id=com.thibaudperso.sonycamera  


This project is a proof of concept to show that it's possible to create your own application for the [rx100 mk2](http://www.sony.co.uk/product/dsc-r-series/dsc-rx100m2)  
For more information read [this thread](https://camera.developer.sony.com/common/forum/en/viewtopic.php?f=21&t=121&start=10#p361)

How to use
----------

You just need to:
* Start Timelapse app
* Put your camera in "Ctrl with Smartphone" mode or Connect it using NFC


Frequently Asked Questions
--------------------------

#### Does this app work with my camera?
Does your camera have a Wifi connection? If not, this app is not compatible.
Check the [following list](https://github.com/ThibaudM/timelapse-sony#cameras-list) to know if your device is compatible. If your device is not on the list and has a Wifi connection, please follow the steps of this tutorial: [Adding a new camera device](https://github.com/ThibaudM/timelapse-sony#adding-a-new-camera-device) part.

#### My camera is automatically set to auto-mode when I use your app. Can I set settings manually?
Sony choose to set the camera settings to auto when you use their WebService. Fortunately on some devices we can set this settings using this same connection. But for the moment, I haven't implemented this feature. List of supported cameras should be: A7R, A7, NEX-5, NEX-6, A5000, A6000, DSC-HX400, DSC-HX60.

#### Does your app exist for iOS?
No, but someone is working on it. You can follow this thread https://plus.google.com/100354636489488589436/posts/Y5P9pwDfXEy

#### Can I use your app by USB?
No, Sony only provides a support using Wifi.

#### When will be the next release? Can you do ... ?
I work on this app when I have free time. So I can't tell you, when will be the next release and if I will implement what you need. The code is opensource. It's up to you to add your features and share them. But you have to know Sony's SDK is very light and doesn't allow to do lots of things (as bulb, hdr...). For more information follow this link: https://developer.sony.com/develop/cameras/

#### This feature doesn't work with my camera, what can I do?
I'm just an indie developer, I only have a RX100 mk2 and sometimes I can test my app on a NEX5-R. For others devices, I can't really test features.

#### Some images are missing during timelapse
It's a known issue. On some devices (like my RX100 mk2) with a normal shutter speed, the camera need lots of time to save the picture and is not able to take a new picture. That's why I recommand to set the interval time at least to 5sec. I hope Sony will provide a new firmware to fix it.


Camera list
------------

#### Working Devices
* A7 (α7)
* DSC-HX400V
* DSC-HX50V
* DSC-HX60
* DSC-HX90
* DSC-HX90V
* DSC-QX10 (Read [this](https://us.en.kb.sony.com/app/answers/detail/a_id/43716/c/65,66/p/40096,90706,90707/) to know where pictures are saved)
* DSC-QX30
* DSC-QX100
* DSC-RX10
* DSC-RX100M2 (known issue with short delays)
* DSC-RX100M3
* DSC-WX80
* DSC-WX350
* FDR-AX100
* FDR-X1000V
* HDR-AZ1
* ILCE-5100 (α5100)
* ILCE-6000 (α6000)
* ILCE-6300 (α6300)
* NEX-5R
* NEX-6


#### Not tested devices
* ILCE-5000 (α5000)
* ILCA-77M2 (α77 II)
* DSC-WX220
* DSC-WX220B
* DSC-HX50
* DSC-HX60V
* DSC-HX90
* DSC-QX1
* FDR-AX100E
* HDR-AS20
* HDR-AS100V
* PXW-SF5


Adding a new camera device
--------------------------

In timelapse-sony application, each Sony camera has an associated API address.  
You can find current associations in the [xml/devices.xml](https://github.com/ThibaudM/timelapse-sony/blob/master/app/src/main/res/xml/devices.xml) file.  
If your device is not listed in this file, don't worry, maybe we can add it.

#### Step 1
Firstly, try with one of the 3 following devices: A5100, RX-10, RX-100 mk2, maybe one works with your camera. In this case, let me know by email what is your camera name and which device you have selected.

#### Step 2
If Step 1, didn't work, 
* Download and install the following application for your Android device: [RetrieveSonyCameraIP.apk](http://thibaud-michel.com/timelapse/RetrieveSonyCameraIP.apk) [[sources](http://thibaud-michel.com/timelapse/RetrieveSonyCameraIP-src.zip)]. You will need to allow applications from unknown sources.
* Connect your camera by Wifi to your Android device.
* Open the app.
* Click on "Start device discovery" and wait.
* If a camera is found, click on it and send the automatic email. If not, send an email with your camera name.
