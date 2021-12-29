package com.apkrom.demo.ads;

import static com.apkrom.demo.Config.TAG;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.apkrom.demo.Config;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import timber.log.Timber;


public class AdmobAds {

	private Activity mActivity = null;
	public AdView mAdView = null;
	private FrameLayout frameLayout;
	private InterstitialAd mInterstitialAd = null;

	public AdmobAds(Activity activity){
		mActivity = activity;
		// Ads Function implementation
		MobileAds.initialize(mActivity, new OnInitializationCompleteListener() {
			@Override
			public void onInitializationComplete(InitializationStatus initializationStatus) {
			}
		});

	}

	public void createAndLoadBanner(FrameLayout frameLayout){
		AdRequest adRequestB = new AdRequest.Builder().build();
		Timber.tag(TAG).e("createAndLoadBanner()");

		mAdView = new AdView(mActivity);
		mAdView.setAdUnitId(Config.firebaseData.getAdmobBannerUnitId());
		mAdView.setAdSize(AdSize.SMART_BANNER);

		mAdView.setVisibility(View.INVISIBLE);
		mAdView.loadAd(adRequestB);

		mAdView.setAdListener(new AdListener() {
			@Override
			public void onAdLoaded() {
				frameLayout.setVisibility(View.VISIBLE);
			}
		});
		frameLayout.addView(mAdView);
	}

	public void show_banner_ad(final boolean show) {
		Timber.tag(TAG).e("show_banner_ad() show = %s", show);
		if (mAdView == null) { return; }

		mActivity.runOnUiThread(new Runnable() {
			public void run() {
				if (show) {
					if (mAdView.isEnabled()) {
						mAdView.setEnabled(true);
					}
					if (mAdView.getVisibility() == View.INVISIBLE) {
						mAdView.setVisibility(View.VISIBLE);
					}
				} else {
					if (mAdView.isEnabled()) { mAdView.setEnabled(false); }
					if (mAdView.getVisibility() != View.INVISIBLE) {
						mAdView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
	}

	public void createInterstitial(int count) {
		String [] UnitIDArray = Config.firebaseData.getAdmobInterUnitID();
		int index = count % UnitIDArray.length;

//		Toast.makeText(mActivity, "index : " + index + "UnitIDArray: "
//				+ UnitIDArray[index] + " length : " + UnitIDArray.length, Toast.LENGTH_LONG).show();

		AdRequest adRequestI = new AdRequest.Builder().build();
		InterstitialAd.load(mActivity,
				UnitIDArray[index],
				adRequestI,
				new InterstitialAdLoadCallback() {
			@Override
			public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
				// The mInterstitialAd reference will be null until
				// an ad is loaded.
				mInterstitialAd = interstitialAd;
				Timber.tag(TAG).i("onAdLoaded");
				interstitialAd.setFullScreenContentCallback(
						new FullScreenContentCallback() {
							@Override
							public void onAdDismissedFullScreenContent() {
								// Called when fullscreen content is dismissed.
								// Make sure to set your reference to null so you don't
								// show it a second time.
								mInterstitialAd = null;
								Timber.tag("TAG").d("The ad was dismissed.");
							}

							@Override
							public void onAdFailedToShowFullScreenContent(AdError adError) {
								// Called when fullscreen content failed to show.
								// Make sure to set your reference to null so you don't
								// show it a second time.
								mInterstitialAd = null;
								Timber.tag("TAG").d("The ad failed to show.");
							}

							@Override
							public void onAdShowedFullScreenContent() {
								// Called when fullscreen content is shown.
								Timber.tag("TAG").d("The ad was shown.");
							}
						});
			}

			@Override
			public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
				// Handle the error
				Timber.tag(TAG).i(loadAdError.getMessage());
				mInterstitialAd = null;
			}
		});
		// Show interstitial
		show_interstitial_ad();
	}

	public void show_interstitial_ad() {
		if (mInterstitialAd != null) {
			mInterstitialAd.show(mActivity);
		} else {
			Timber.tag("TAG").d("The interstitial ad wasn't ready yet.");
		}
	}

	public interface CallbackInterstitial {
		void onFailedToLoad();
		void onAdLoaded();
	}

	public void onMainPause () {
		if (mAdView != null) { mAdView.pause(); }
	}

	public void onMainResume () {
		if (mAdView != null) { mAdView.resume(); }
	}

	public void onMainDestroy () {
		if (mAdView != null) { mAdView.destroy(); }
	}
}
