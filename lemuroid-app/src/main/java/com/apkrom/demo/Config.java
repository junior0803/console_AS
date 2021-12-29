package com.apkrom.demo;

import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class Config {
	public static final int SPLASH_DISPLAY_DURATION = 3000; //if you want change time of splash activity (in millisecond)

    public static final String TAG = "apkrom-demo";

    //Select "true" in your favorite choice and "false" in another choices.
    public static final boolean offlineDataJson = true;  //if you want put your data in assets
    public static final boolean onlineFirebaseDataJson = false;  //if you want put your data in firebase

    // ** for put you privacy policy go to utils.Utils.PrivacyPolicy (text or html) ** //
    public static FirebaseData firebaseData;

    public static final String Admob = "admob";
    public static final String Facebook = "facebook";
    public static final String Appodeal = "appodeal";
    public static final String Unity = "unity";

	public static String ConvertUrl(String url){
		byte[] data = Base64.decode(url, Base64.DEFAULT);
		return new String(data, StandardCharsets.UTF_8);
	}
}
