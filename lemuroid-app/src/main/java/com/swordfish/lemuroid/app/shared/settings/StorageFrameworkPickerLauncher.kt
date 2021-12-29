package com.swordfish.lemuroid.app.shared.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.apkrom.demo.R
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import com.swordfish.lemuroid.lib.android.RetrogradeActivity
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import timber.log.Timber
import javax.inject.Inject

class StorageFrameworkPickerLauncher : RetrogradeActivity() {

    @Inject lateinit var directoriesManager: DirectoriesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                this.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                this.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                this.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                this.addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
                this.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            try {
                startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER)
            } catch (e: Exception) {
                showStorageAccessFrameworkNotSupportedDialog()
            }
        }
    }

    private fun showStorageAccessFrameworkNotSupportedDialog() {
        val message = getString(R.string.dialog_saf_not_found, directoriesManager.getInternalRomsDirectory())
        val actionLabel = getString(R.string.ok)
        displayErrorDialog(message, actionLabel) { finish() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {

            val sharedPreferences = SharedPreferencesHelper.getLegacySharedPreferences(this)
            val preferenceKey = getString(R.string.pref_key_extenral_folder)

            val currentValue = resultData?.data
            val gameDirectory = gameDirectoryUri + applicationContext.packageName;

            val newValue = Uri.parse(gameDirectory)

            Timber.tag("junior").e("newValue = %s", newValue)
            Timber.tag("junior").e("currentValue = %s", currentValue)

            if (newValue != null  && currentValue != null
                && gameDirectory == currentValue.toString()
            ) {
                updatePersistableUris(currentValue)
                sharedPreferences.edit().apply {
                    this.putString(preferenceKey, newValue.toString())
                    this.apply()
                }
                startLibraryIndexWork()
            } else {
                Toast.makeText(this, "Please select directory correctly", Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }

    private fun updatePersistableUris(uri: Uri) {
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

    private fun startLibraryIndexWork() {
        LibraryIndexScheduler.scheduleFullSync(applicationContext)
    }

    companion object {
        private const val REQUEST_CODE_PICK_FOLDER = 1
        private const val gameDirectoryUri = "content://com.android.externalstorage.documents/tree/primary%3ADownload%2FApkRom%2F"

        fun pickFolder(context: Context) {
            context.startActivity(Intent(context, StorageFrameworkPickerLauncher::class.java))
        }
    }
}
