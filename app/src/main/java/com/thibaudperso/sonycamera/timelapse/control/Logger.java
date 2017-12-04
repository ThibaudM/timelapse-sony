package com.thibaudperso.sonycamera.timelapse.control;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.thibaudperso.sonycamera.timelapse.TimelapseApplication;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by thibaud on 11/09/2017.
 */

public class Logger {

    private final static SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);

    private static String logs = "";

    public static void d(String message) {
        m("DEBUG - " + message);
    }

    public static void e(String message) {
        m("ERROR - " + message);
    }

    private static void m(String message) {

        logs += DATE_FORMAT.format(Calendar.getInstance().getTime()) + " (" +
                android.os.Process.myPid() + ") - " + message + "\n";

        Log.v("DEBUG", message);
    }

    private static String getLogsString(Context context) {
        DeviceManager dm = ((TimelapseApplication) context.getApplicationContext()).getDeviceManager();

        return dm.getSelectedDevice().toLongString()+"\n\n"+logs;

    }


    public static void sendEmail(Context context) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"thibaud.michel@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, "[Timelapse Sony] Troubleshooting");
        i.putExtra(Intent.EXTRA_TEXT, getLogsString(context));
        try {
            context.startActivity(Intent.createChooser(i, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "There are no email clients installed.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public static String intentToString(Intent intent) {
        if (intent == null)
            return "";

        StringBuilder stringBuilder = new StringBuilder("action: ")
                .append(intent.getAction())
                .append(" data: ")
                .append(intent.getDataString())
                .append(" extras: ")
                ;
        for (String key : intent.getExtras().keySet())
            stringBuilder.append(key).append("=").append(intent.getExtras().get(key)).append(" ");

        return stringBuilder.toString();

    }
}
