package com.swordfish.lemuroid.app.mobile.feature.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.apkrom.demo.Config
import com.apkrom.demo.R
import com.apkrom.demo.ads.AdmobAds
import com.apkrom.demo.ads.FacebookAds
import com.apkrom.demo.ads.IronSourceAds
import com.apkrom.demo.ads.UnitysAds
import com.apkrom.demo.utils.GetOfflineData
import com.apkrom.demo.utils.GetOutData
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.swordfish.lemuroid.app.mobile.feature.favorites.FavoritesFragment
import com.swordfish.lemuroid.app.mobile.feature.games.GamesFragment
import com.swordfish.lemuroid.app.mobile.feature.home.HomeFragment
import com.swordfish.lemuroid.app.mobile.feature.search.SearchFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.AdvancedSettingsFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.BiosSettingsFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.CoresSelectionFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.GamepadSettingsFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.SaveSyncFragment
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsFragment
import com.swordfish.lemuroid.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.swordfish.lemuroid.app.mobile.feature.systems.MetaSystemsFragment
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.GameLauncher
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.main.BusyActivity
import com.swordfish.lemuroid.app.shared.main.PostGameHandler
import com.swordfish.lemuroid.app.shared.savesync.SaveSyncWork
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import com.swordfish.lemuroid.ext.feature.review.ReviewManager
import com.swordfish.lemuroid.lib.android.RetrogradeAppCompatActivity
import com.swordfish.lemuroid.lib.injection.PerActivity
import com.swordfish.lemuroid.lib.injection.PerFragment
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.common.view.setVisibleOrGone
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MainActivity : RetrogradeAppCompatActivity(), BusyActivity {

    // Junior patch begin
    private val TAG = "GameActivity"

    var count = 0

    private var admobAd: AdmobAds? = null
    private var facebookAd: FacebookAds? = null
    private var unityAd: UnitysAds? = null
    private var ironSourceAds: IronSourceAds? = null
    // Junior patch end

    @Inject lateinit var postGameHandler: PostGameHandler
    @Inject lateinit var saveSyncManager: SaveSyncManager

    private val reviewManager = ReviewManager()
    private var mainViewModel: MainViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeActivity()
        // Junior patch begin
        if (Config.firebaseData != null) {
            Timber.tag("junior").e("response = %s", Config.firebaseData)
            if (Config.firebaseData.admobEnable) admobAd = AdmobAds(this@MainActivity)
            if (Config.firebaseData.facebookEnable) facebookAd = FacebookAds(this@MainActivity)
            if (Config.firebaseData.unityEnable) unityAd = UnitysAds(this@MainActivity)
            if (Config.firebaseData.mediationIS) ironSourceAds = IronSourceAds(this@MainActivity)

            createInterstitialAds()
            setScheduleInterstitial()
        }
        // Junior patch end
    }

    // Junior patch begin
    private fun readData() {
        if (Config.offlineDataJson) {
            GetOfflineData(this).loadAllData()
        } else if (Config.onlineFirebaseDataJson) {
            GetOutData(this).loadAllData()
        }
    }
    private fun createInterstitialAds() {
        Timber.tag(TAG).e("GameActivity - createInterstitialAds()")
        if (Config.firebaseData != null) {
            if (Config.firebaseData.mediationIS) {
                ironSourceAds!!.createInterstitial()
            }
            if (Config.firebaseData.unityEnable) {
                unityAd!!.createInterstitial()
            }
        }
    }

    private fun showInterstitialAds(count: Int) {
        Timber.tag(TAG).e("GameActivity - showInterstitialAds()")
        if (Config.firebaseData != null) {
            if (Config.firebaseData.mediationIS) {
                ironSourceAds!!.show_interstitial_ad()
            }
            if (Config.firebaseData.admobEnable) {
                admobAd!!.createInterstitial(count)
            }
            if (Config.firebaseData.facebookEnable) {
                facebookAd!!.createInterstitial(count)
            }
            if (Config.firebaseData.unityEnable) {
                unityAd!!.show_interstitial_ad()
            }
        }
    }

    private fun setScheduleInterstitial() {
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        val periodTime = Config.firebaseData.adsInterval
        scheduler.scheduleAtFixedRate({
            Timber.tag(TAG).e("run()")
            runOnUiThread { //Ads function Implementation
                showInterstitialAds(count)
                count++
            }
        }, 0, periodTime.toLong(), TimeUnit.SECONDS)
    }
    // Junior patch end

    override fun activity(): Activity = this
    override fun isBusy(): Boolean = mainViewModel?.displayProgress?.value ?: false

    private fun initializeActivity() {
        setSupportActionBar(findViewById(R.id.toolbar))

        reviewManager.initialize(applicationContext)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

        val topLevelIds = setOf(
            R.id.navigation_home,
            R.id.navigation_settings
        )
        val appBarConfiguration = AppBarConfiguration(topLevelIds)

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        mainViewModel = ViewModelProvider(this, MainViewModel.Factory(applicationContext))
            .get(MainViewModel::class.java)

        mainViewModel?.displayProgress?.observe(this) { isRunning ->
            findViewById<ProgressBar>(R.id.progress).setVisibleOrGone(isRunning)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Timber.tag("junior").e("requestCode : $requestCode resultCode : $resultCode")
        when (requestCode) {
            BaseGameActivity.REQUEST_PLAY_GAME -> {
                postGameHandler.handle(true, this, resultCode, data)
                    .subscribeBy(Timber::e) { }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val isSupported = saveSyncManager.isSupported()
        val isConfigured = saveSyncManager.isConfigured()
        menu?.findItem(R.id.menu_options_sync)?.isVisible = isSupported && isConfigured
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mobile_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_options_help -> {
                displayLemuroidHelp()
                true
            }
            R.id.menu_options_sync -> {
                SaveSyncWork.enqueueManualWork(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun displayLemuroidHelp() {
        val systemFolders = SystemID.values()
            .map { it.dbname }
            .map { "<i>$it</i>" }
            .joinToString(", ")

        val message = getString(R.string.lemuroid_help_content).replace("\$SYSTEMS", systemFolders)
        AlertDialog.Builder(this)
            .setMessage(Html.fromHtml(message))
            .show()
    }

    override fun onSupportNavigateUp() = findNavController(R.id.nav_host_fragment).navigateUp()

    @dagger.Module
    abstract class Module {

        @PerFragment
        @ContributesAndroidInjector(modules = [SettingsFragment.Module::class])
        abstract fun settingsFragment(): SettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [GamesFragment.Module::class])
        abstract fun gamesFragment(): GamesFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [MetaSystemsFragment.Module::class])
        abstract fun systemsFragment(): MetaSystemsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [HomeFragment.Module::class])
        abstract fun homeFragment(): HomeFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [SearchFragment.Module::class])
        abstract fun searchFragment(): SearchFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [FavoritesFragment.Module::class])
        abstract fun favoritesFragment(): FavoritesFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [GamepadSettingsFragment.Module::class])
        abstract fun gamepadSettings(): GamepadSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [BiosSettingsFragment.Module::class])
        abstract fun biosInfoFragment(): BiosSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [AdvancedSettingsFragment.Module::class])
        abstract fun advancedSettingsFragment(): AdvancedSettingsFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [SaveSyncFragment.Module::class])
        abstract fun saveSyncFragment(): SaveSyncFragment

        @PerFragment
        @ContributesAndroidInjector(modules = [CoresSelectionFragment.Module::class])
        abstract fun coresSelectionFragment(): CoresSelectionFragment

        @dagger.Module
        companion object {

            @Provides
            @PerActivity
            @JvmStatic
            fun settingsInteractor(activity: MainActivity, directoriesManager: DirectoriesManager) =
                SettingsInteractor(activity, directoriesManager)

            @Provides
            @PerActivity
            @JvmStatic
            fun gameInteractor(
                activity: MainActivity,
                retrogradeDb: RetrogradeDatabase,
                shortcutsGenerator: ShortcutsGenerator,
                gameLauncher: GameLauncher
            ) =
                GameInteractor(activity, retrogradeDb, false, shortcutsGenerator, gameLauncher)
        }
    }
}
