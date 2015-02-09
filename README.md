Timelapse - Sony Camera
===================

Want [Donate](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=LFFFQZD9UNKRC&lc=FR&item_name=Thibaud%20Michel&item_number=1&currency_code=EUR&bn=PP-DonationsBF%3abtn_donate_LG%2egif%3aNonHosted)?  

Project type: **Android**  
Current version: **2.0.2**  
Google Play store link: https://play.google.com/store/apps/details?id=com.thibaudperso.timelapse  


This project is a proof of concept to show it's possible to create your own application for [rx100 mk2](http://www.sony.co.uk/product/dsc-r-series/dsc-rx100m2)  
For more information read [this thread](https://camera.developer.sony.com/common/forum/en/viewtopic.php?f=21&t=121&start=10#p361)

How to use
----------

You just need to:
* Start Timelapse app
* Put your camera in "Ctrl with Smartphone" mode or Connect it by NFC


Frequently Asked Questions
----------

#### Is this app works with my camera?
Is your camera has a Wifi connection ? If not this app is not compatible.
Check the following list to know if your device is compatible: A5100, A6000, Alpha 7, DSC-HX50V, DSC-HX400V, DSC-WX80, NEX-5R, NEX-6, QX-10, QX-30, QX-100, RX-10, RX-100 mk3. If your device is not in the list and has a Wifi connection try another device when you start app.

#### My camera is automatically set to auto-mode when I use your app. Can I set settings manually?
Sony choosed to set the camera settings to auto when you use their WebService. Fortunately on some devices we can set this settings using this same connection. But for the moment I haven't implemented this feature. List of supported cameras should be: A7R, A7, NEX-5, NEX-6, A5000, A6000, DSC-HX400, DSC-HX60.

#### Is your app exists for iOS?
No, but someone is working on. You can follow this thread https://plus.google.com/100354636489488589436/posts/Y5P9pwDfXEy

#### Can I use your app by USB?
No, Sony only provides a support using Wifi.

#### When will be the next release? Can you do ... ?
I work for this app when I have free time. So, I can't tell you when will be the next release and if I will implement what you need. The code is opensource it's up to you to add your features and share them. But you have to know Sony's SDK is very light and doesn't allow to do lots of things (as bulb, hdr...). For more information follow this link: https://developer.sony.com/develop/cameras/

#### This feature doesn't work with my camera, what can I do?
I'm just an indie developer, I only have a RX100 mk2 and sometimes I can test my app on a NEX5-R. For others devices, I can't really test features.

#### Some images are missing during timelapse
It's a known issue. On some devices (like my RX100 mk2) with a normal shutter speed, camera needs lots of time to save the picture and is not able to take a new picture. That's why I recommand to set interval at least at 5sec. I hope Sony will provide a new firmware to fix it.
