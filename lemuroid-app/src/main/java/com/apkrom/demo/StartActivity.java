package com.apkrom.demo;

import static com.apkrom.demo.Config.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.apkrom.demo.ads.AdmobAds;
import com.apkrom.demo.ads.FacebookAds;
import com.apkrom.demo.ads.IronSourceAds;
import com.apkrom.demo.ads.UnitysAds;
import com.apkrom.demo.utils.Utils;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.IronSourceBannerLayout;
import com.apkrom.demo.R;
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity;
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Time;

import timber.log.Timber;

public class StartActivity extends AppCompatActivity {
	private static final int PERMISSION_REQUEST_CODE = 2296;
	private Button playButton;

	private ProgressBar progressBar;
	private TextView progressText;
	//IronSource
	private AdmobAds admobAd;
	private FacebookAds facebookAd;
	private UnitysAds unityAd;
	private IronSourceAds ironSourceAds;
	private Context mContext;

	private FrameLayout mBannerParentLayout;
	private IronSourceBannerLayout mIronSourceBannerLayout;

	private boolean online = false;

	private AssetManager assetManager;
	private InputStream inputStream = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;

		setContentView(R.layout.activity_lemuroid);
		// Play Asset Delivery Init-Time init
		initInstallTime();

		// Background Image Customize
		FrameLayout imageView =  findViewById(R.id.adsLayout);

		Drawable background = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomBackground());
		imageView.setBackground(background);

		Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);

		// playbutton Customize
		playButton = findViewById(R.id.play_button);

		Drawable playbutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomPlayButton());
		playButton.setBackground(playbutton);

		//moreButton Customize
		Button moreButton = findViewById(R.id.moregame_button);

		Drawable morebutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomMoreButton());
		moreButton.setBackground(morebutton);

		//policyButton Customzie
		Button policyButton = findViewById(R.id.policy_button);
		Drawable policybutton = new BitmapDrawable(getResources(),
				Config.firebaseData.getCustomPolicyButton());
		policyButton.setBackground(policybutton);

		progressBar = findViewById(R.id.progress);
		progressText = findViewById(R.id.progressText);

		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {

			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		imageView.startAnimation(animation);
		playButton.startAnimation(animation);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startGame();
			}
		});

		moreButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.ConvertUrl(Utils.moreGameInfoUri)));
				startActivity(browserIntent);
			}
		});

		policyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.ConvertUrl(Utils.privacyPolicyUri)));
				startActivity(browserIntent);
			}
		});

		if (com.apkrom.demo.Config.firebaseData.getAdmobEnable())
			admobAd = new AdmobAds(StartActivity.this);
		if (Config.firebaseData.getFacebookEnable())
			facebookAd = new FacebookAds(StartActivity.this);
		if (Config.firebaseData.getUnityEnable())
			unityAd = new UnitysAds(StartActivity.this);
		if (Config.firebaseData.getMediationIS())
			ironSourceAds = new IronSourceAds(StartActivity.this);

		mBannerParentLayout = findViewById(R.id.banner_footer);

		createBannerAds();
		createInterstitialAds();
	}

	public final void startGame() {
		String gamePath = Decompress.getExtensionPath(this);
		Timber.tag(TAG).e("gamePath = %s", gamePath);
		if (gamePath == null || gamePath.length() == 0) {
			makeGameData();
		} else {
			openGame();
		}
	}

	private void openGame() {
		Intent intent = new Intent();
		intent.setClass(this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	private void makeGameData(){

		Timber.tag(TAG).e("makeGameData()");

		String[] list = new String[0];
		try {
			list = getAssets().list("data");
		} catch (IOException e) {
			e.printStackTrace();
		}

		String dataStr = null;
		if (list != null) {
			int length = list.length;
			int i = 0;
			while (true) {
				if (i >= length) {
					break;
				}
				String str = list[i];
				if (str.endsWith(".zip")) {
					dataStr = str;
					break;
				}
				i++;
			}
		}

		if (dataStr == null){
			if (Config.offlineDataJson){
				Toast.makeText(mContext, R.string.data_failed, Toast.LENGTH_LONG).show();
				return;
			} else {
				dataStr = Config.firebaseData.getIsoGame();
				online = true;
			}
		}

		Timber.tag(TAG).e("dataStr = %s", dataStr);
		unzip(dataStr);

		// Junior patch begin
		String gameDirectory = "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FApkRom%2F" + this.getPackageName();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		String preferenceKey = getString(R.string.pref_key_extenral_folder);
		sharedPreferences.edit().putString(preferenceKey, gameDirectory).apply();
		// Junior patch end
	}

	private void unzip(String dataStr) {
		progressBar.setVisibility(View.VISIBLE);
		progressBar.setMax(100);
		new unzipThread().execute(dataStr);
	}

	private class unzipThread extends AsyncTask<String, Integer, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBar.setVisibility(View.VISIBLE);
			progressBar.setProgress(0);
			progressText.setVisibility(View.VISIBLE);
			progressText.setText(R.string.loding);
			playButton.setEnabled(false);
		}

		@Override
		protected String doInBackground(String... strings) {
			if (online){
				//Decompress.unzipFromUrl(getApplicationContext(), strings[0],
				//		Decompress.getGameFolder(getApplicationContext()), progressBar);
				getInputStreamFromAssetManager(progressBar);
			} else {
				Decompress.unzipFromAssets(mContext, "data/" + strings[0],
						Decompress.getGameFolder(mContext), progressBar);
			}
			return strings[0];
		}

		@Override
		protected void onPostExecute(String s) {
			super.onPostExecute(s);
			progressBar.setProgress(100);
			progressBar.setVisibility(View.INVISIBLE);
			progressText.setVisibility(View.VISIBLE);
			progressText.setText(R.string.successload);
			playButton.setEnabled(true);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (Config.firebaseData != null){
			if (ironSourceAds != null){
				ironSourceAds.onMainResume();
			}
			if (admobAd != null){
				admobAd.onMainResume();
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		// call the IronSource onPause method
		if (Config.firebaseData != null){
			if (ironSourceAds != null){
				ironSourceAds.onMainPause();
			}
			if (admobAd != null){
				admobAd.onMainPause();
			}
		}
	}

	/**
	 * Destroys IronSource Banner and removes it from the container
	 *
	 */
	private void destroyAndDetachBanner() {
		IronSource.destroyBanner(mIronSourceBannerLayout);
		if (mBannerParentLayout != null) {
			mBannerParentLayout.removeView(mIronSourceBannerLayout);
		}
	}

	//Banner : Start
	public void createBannerAds(){
		Timber.tag(TAG).e("createBannerAds()");
		if (Config.firebaseData != null){
			if (Config.firebaseData.getMediationIS()){
				ironSourceAds.createAndLoadBanner(mBannerParentLayout);
				ironSourceAds.show_banner_ad(true);
			}
			if (com.apkrom.demo.Config.firebaseData.getAdmobEnable()){
				admobAd.createAndLoadBanner(mBannerParentLayout);
				admobAd.show_banner_ad(true);
			}
			if (Config.firebaseData.getFacebookEnable()){
				facebookAd.createAndLoadBanner(mBannerParentLayout);
				facebookAd.show_banner_ad(true);
			}
			if (Config.firebaseData.getUnityEnable()){
				unityAd.createAndLoadBanner(mBannerParentLayout);
			}
		}
	}
	//Banner : End

	//InterstitialAd : Start
	public void createInterstitialAds(){
		Timber.tag(TAG).e("createInterstitialAds()");
		if (Config.firebaseData != null){
			if (Config.firebaseData.getMediationIS()){
				ironSourceAds.createInterstitial();
			}
			if (com.apkrom.demo.Config.firebaseData.getAdmobEnable()){
				admobAd.createInterstitial(0);
			}
			if (Config.firebaseData.getFacebookEnable()){
				facebookAd.createInterstitial(0);
			}
			if (Config.firebaseData.getUnityEnable()) {
				unityAd.createInterstitial();
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				if (Environment.isExternalStorageManager()) {
					// perform action when allow permission success
					Toast.makeText(this, "write storage permission allowed!", Toast.LENGTH_SHORT).show();

				} else {
					Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0) {
				boolean READ_EXTERNAL_STORAGE = grantResults[0] == PackageManager.PERMISSION_GRANTED;
				boolean WRITE_EXTERNAL_STORAGE = grantResults[1] == PackageManager.PERMISSION_GRANTED;

				if (READ_EXTERNAL_STORAGE && WRITE_EXTERNAL_STORAGE) {
					// perform action when allow permission success
					Toast.makeText(this, "write storage permission allowed!", Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}


	/**
	 * start install-time delivery mode
	 */
	private void initInstallTime() {
		try {
			Context context = createPackageContext("com.apkrom.demo", 0);
			// or please use follow code
			//Context context = createPackageContext(getApplicationContext().getPackageName(), 0);
			assetManager = context.getAssets();
			Timber.tag("junior").e("assetManager : %s", assetManager);
			Timber.tag("junior").e("package Name : %s", context.getPackageName());
		} catch (PackageManager.NameNotFoundException e) {
			Timber.tag("junior").e("NameNotFoundException");
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to get Input Stream from the provided filepath
	 */
	private void getInputStreamFromAssetManager(ProgressBar progressBar) {
		try {
			inputStream = assetManager.open(Utils.assetFileName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (inputStream != null) {
			Decompress.unzipFromGoogleAsset(mContext, inputStream,
					Decompress.getGameFolder(mContext), progressBar);
			//writeDataToFile(inputStream, progressBar);
		} else {
			Timber.tag(TAG).e("inputStream is null");
		}
	}

	/**
	 * This method will create a new hidden temporary file & write datafrom inputstrem to temporary
	 * file, so as to play video from file.
	 * Note : If you do not want hidden file then just remove ".(dot)" from prefix of fileName
	 */
	private void writeDataToFile(InputStream inputStream, ProgressBar progressBar) {
		File file = new File(mContext.getObbDir(), "main.83." + mContext.getPackageName() + ".iso");
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream outputStream = new FileOutputStream(file, false);
			int read;
			byte[] bytes = new byte[8192];
			if (inputStream != null) {
				int size = inputStream.available();
				while ((read = inputStream.read(bytes)) != -1) {
					int progress = (int) ((((float) 75) * ((float) read)) / ((float) size));
					if (progressBar != null) {
						progressBar.setProgress(progress);
					}
					outputStream.write(bytes, 0, read);
				}
			}
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
