package com.apkrom.demo.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import timber.log.Timber;

/**
 * Created by Ravi Tamada on 01/09/16.
 * www.androidhive.info
 */
public class HttpHandler {

	private static final String TAG = HttpHandler.class.getSimpleName();

	public HttpHandler() {
	}

	public String makeServiceCall(String reqUrl) {
		String response = null;
		try {
			URL url = new URL(reqUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			// read the response
			InputStream in = new BufferedInputStream(conn.getInputStream());
			response = convertStreamToString(in);
		} catch (MalformedURLException e) {
			Timber.tag(TAG).e("MalformedURLException: %s", e.getMessage());
		} catch (ProtocolException e) {
			Timber.tag(TAG).e("ProtocolException: %s", e.getMessage());
		} catch (IOException e) {
			Timber.tag(TAG).e("IOException: %s", e.getMessage());
		} catch (Exception e) {
			Timber.tag(TAG).e("Exception: %s", e.getMessage());
		}
		return response;
	}

	public Bitmap makeDrawable(String reqUrl) {
		Bitmap response = null;
		try {
			URL url = new URL(reqUrl);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			// read the response
			InputStream in = new BufferedInputStream(conn.getInputStream());
			response = BitmapFactory.decodeStream(in);
		} catch (MalformedURLException e) {
			Timber.tag(TAG).e("MalformedURLException: %s", e.getMessage());
		} catch (ProtocolException e) {
			Timber.tag(TAG).e("ProtocolException: %s", e.getMessage());
		} catch (IOException e) {
			Timber.tag(TAG).e("IOException: %s", e.getMessage());
		} catch (Exception e) {
			Timber.tag(TAG).e("Exception: %s", e.getMessage());
		}
		return response;
	}

	private String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}
}
