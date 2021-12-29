package com.swordfish.lemuroid.app.mobile.feature.home

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.Carousel
import com.apkrom.demo.R
import com.swordfish.lemuroid.app.shared.GameInteractor
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.shared.settings.SettingsInteractor
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber
import javax.inject.Inject

class HomeFragment : Fragment() {

    @Inject lateinit var retrogradeDb: RetrogradeDatabase
    @Inject lateinit var gameInteractor: GameInteractor
    @Inject lateinit var settingsInteractor: SettingsInteractor

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("StringFormatMatches")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Junior patch begin
        val sharedPreferences = SharedPreferencesHelper.getLegacySharedPreferences(requireContext())
        val preferenceKey = getString(R.string.pref_key_extenral_folder)
        val currentValue: String? = sharedPreferences.getString(preferenceKey, null)
        if (currentValue == null) {
            val builder = AlertDialog.Builder(context)
            //Uncomment the below code to Set the message and title from the strings.xml file
            builder.setMessage(R.string.missing_permission)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.first_permission_dialog, requireContext().packageName))
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, _ ->
                    handleChangeExternalFolder()
                    dialog.cancel()
                }
            builder.create().show();
        }

        // Junior patch begin
        val gameDirectory =
            "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FApkRom%2F" + requireContext().packageName
        updatePersistableUris(Uri.parse(gameDirectory))
        LibraryIndexScheduler.scheduleFullSync(context)
        //Junior patch end

        val homeViewModel =
            ViewModelProvider(
                this,
                HomeViewModel.Factory(requireContext().applicationContext, retrogradeDb)
            ).get(HomeViewModel::class.java)

        // Disable snapping in carousel view
        Carousel.setDefaultGlobalSnapHelperFactory(null)

        val pagingController = EpoxyHomeController(gameInteractor, settingsInteractor)

        val recyclerView = view.findViewById<RecyclerView>(R.id.home_recyclerview)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = pagingController.adapter

        homeViewModel.recentGames.observe(viewLifecycleOwner) {
            pagingController.updateRecents(it)
        }

        homeViewModel.favoriteGames.observe(viewLifecycleOwner) {
            pagingController.updateFavorites(it)
        }

        homeViewModel.discoverGames.observe(viewLifecycleOwner) {
            pagingController.updateDiscover(it)
        }

        homeViewModel.indexingInProgress.observe(viewLifecycleOwner) {
            pagingController.updateLibraryIndexingInProgress(it)
        }
    }

    private fun updatePersistableUris(uri: Uri) {
        val contentResolver = requireContext().contentResolver
        Timber.tag("junior").e("updatePersistableUris uri : %s", uri)
        contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .filter { it.uri != uri }
            .forEach {
                Timber.tag("junior").e("updatePersistableUris - If")
                contentResolver.releasePersistableUriPermission(
                    it.uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        Timber.tag("junior").e("updatePersistableUris - permission")
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    private fun handleChangeExternalFolder() {
        settingsInteractor.changeLocalStorageFolder()
    }

    @dagger.Module
    class Module
}
