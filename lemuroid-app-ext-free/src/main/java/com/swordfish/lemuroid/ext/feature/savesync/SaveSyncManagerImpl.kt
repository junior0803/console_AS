package com.swordfish.lemuroid.ext.feature.savesync

import android.app.Activity
import android.content.Context
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.savesync.SaveSyncManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import io.reactivex.Completable

class SaveSyncManagerImpl(
    private val appContext: Context,
    private val directoriesManager: DirectoriesManager
) : SaveSyncManager {
    override fun getProvider(): String = ""

    override fun getSettingsActivity(): Class<out Activity>? = null

    override fun isSupported(): Boolean = false

    override fun isConfigured(): Boolean = false

    override fun getLastSyncInfo(): String = ""

    override fun getConfigInfo(): String = ""

    override fun sync(cores: Set<CoreID>) = Completable.complete()

    override fun computeSavesSpace() = ""

    override fun computeStatesSpace(coreID: CoreID) = ""
}
