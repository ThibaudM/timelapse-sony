package com.thibaudperso.sonycamera.timelapse;

import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.text.Spanned;

public class Utils {

	public static Bitmap downloadBitmap(String url) {

	     final DefaultHttpClient client = new DefaultHttpClient();
	     
	     Bitmap image = null;

	     final HttpGet getRequest = new HttpGet(url);
	     try {

	         HttpResponse response = client.execute(getRequest);

	         final int statusCode = response.getStatusLine().getStatusCode();

	         if (statusCode != HttpStatus.SC_OK) {
	             return null;
	         }

	         final HttpEntity entity = response.getEntity();
	         if (entity != null) {
	             InputStream inputStream = null;
	             try {
	                 inputStream = entity.getContent();
	                 image = BitmapFactory.decodeStream(inputStream);


	             } finally {
	                 if (inputStream != null) {
	                     inputStream.close();
	                 }
	                 entity.consumeContent();
	             }
	         }
	     } catch (Exception e) {
	         getRequest.abort();
	     } 

	     return image;
	 }
	
	public static Spanned htmlColorizeFirstLetter(String text, int color) {
		
		if(text == null || text.length() <= 2) {
			throw new IllegalStateException();
		}
		
		String strColor = String.format("#%06X", 0xFFFFFF & color);
		
		String output = "<font color='"+strColor+"'>";
		output += text.charAt(0)+"</font>";
		output += text.substring(1);
		
		return Html.fromHtml(output);
	}
	
}
