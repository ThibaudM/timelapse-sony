package com.thibaudperso.sonycamera.timelapse.control;

import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

import com.thibaudperso.sonycamera.R;
import com.thibaudperso.sonycamera.sdk.model.Device;
import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class DeviceManager {

    private final static String FILENAME = "device.xml";

    private static final String NAMESPACE = null;
    private static final String CAMERA_ID_PREFERENCE = "pref_camera_id";

    private TimelapseApplication mApplication;

    private List<Device> mDevicesList;
    private Device mSelectedDevice;

    public DeviceManager(TimelapseApplication application) {
        mApplication = application;
        mDevicesList = new ArrayList<>();

        final SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(application);
        int currentCameraId = preferences.getInt(CAMERA_ID_PREFERENCE, -1);

        sendXmlFileToInternalStorageIfNotExists();
        parseCameraModels();


        for (Device device : mDevicesList) {
            if (device.getId() == currentCameraId) {
                mSelectedDevice = device;
                return;
            }
        }

        if (mDevicesList.size() == 0) {
            throw new RuntimeException("No camera in the list");
        }
        mSelectedDevice = mDevicesList.get(0);
        mApplication.getCameraAPI().setDevice(mDevicesList.get(0));
    }

    private void sendXmlFileToInternalStorageIfNotExists() {

        File file = new File(mApplication.getFilesDir(), FILENAME);
        if (file.exists()) {
            return;
        }

        InputStream is = mApplication.getResources().openRawResource(R.raw.devices);
        try {
            FileOutputStream os = new FileOutputStream(file);
            try {
                byte[] buffer = new byte[1024];
                int len = is.read(buffer);
                while (len != -1) {
                    os.write(buffer, 0, len);
                    len = is.read(buffer);
                }
                os.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void getLastListOfDevicesFromInternet(final Listener listener) {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(mApplication.getFilesDir(), FILENAME);

                try {
                    URL url = new URL(mApplication.getString(R.string.camera_xml_url));

                    URLConnection ucon = url.openConnection();
                    InputStream is = ucon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] data = new byte[50];
                    int current;
                    while ((current = bis.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, current);
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(buffer.toByteArray());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                parseCameraModels();

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDevicesListChanged(mDevicesList);

                    }
                });
            }
        }).start();
    }


    public List<Device> getDevices() {
        return mDevicesList;
    }

    public Device getSelectedDevice() {
        return mSelectedDevice;
    }

    public void setSelectedDevice(Device selectedDevice) {
        // Be sure it's not a copy
        for (Device device : mDevicesList) {
            if (device.equals(selectedDevice)) {

                final SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(mApplication);
                preferences.edit().putInt(CAMERA_ID_PREFERENCE, device.getId()).apply();

                mSelectedDevice = device;

                mApplication.getCameraAPI().setDevice(device);

                return;
            }
        }
    }

	
	/*
     * XML parser
	 */

    private void parseCameraModels() {
        try {
            XmlPullParserFactory xppf = XmlPullParserFactory.newInstance();
            xppf.setNamespaceAware(true);
            XmlPullParser xpp = xppf.newPullParser();
            InputStream is = new FileInputStream(new File(mApplication.getFilesDir(), FILENAME));
            xpp.setInput(is, null);
            parseCameraModels(xpp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseCameraModels(XmlPullParser parser) {

        mDevicesList.clear();

        try {

            //parser.require(XmlPullParser.START_TAG, NAMESPACE, "cameras");
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();

                if (name.equals("camera")) {
                    mDevicesList.add(readCamera(parser));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Device readCamera(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, NAMESPACE, "camera");

        int id = -1;
        String deviceModel = null;
        String webService = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "id":
                    id = readId(parser);
                    break;
                case "model":
                    deviceModel = readModel(parser);
                    break;
                case "webservice":
                    webService = readWebService(parser);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

        return new Device(id, deviceModel, webService);
    }

    private int readId(XmlPullParser parser) throws IOException, XmlPullParserException {
        int id = -1;

        parser.require(XmlPullParser.START_TAG, NAMESPACE, "id");
        try {
            id = Integer.parseInt(readText(parser));
        } catch (Exception ignored) {
        }
        parser.require(XmlPullParser.END_TAG, NAMESPACE, "id");

        return id;
    }

    private String readModel(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, NAMESPACE, "model");
        String model = readText(parser);
        parser.require(XmlPullParser.END_TAG, NAMESPACE, "model");

        return model;
    }

    private String readWebService(XmlPullParser parser) throws IOException, XmlPullParserException {

        parser.require(XmlPullParser.START_TAG, NAMESPACE, "webservice");
        String webService = readText(parser);
        parser.require(XmlPullParser.END_TAG, NAMESPACE, "webservice");

        return webService;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


    public interface Listener {
        void onDevicesListChanged(List<Device> devices);
    }

}
