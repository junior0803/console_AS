package com.swordfish.lemuroid.lib.saves

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import io.reactivex.Completable
import io.reactivex.Maybe
import java.io.File
import java.io.FileOutputStream

class StatesPreviewManager(private val directoriesManager: DirectoriesManager) {

    fun getPreviewForSlot(
        game: Game,
        coreID: CoreID,
        index: Int,
        size: Int
    ) = Maybe.fromCallable {
        val screenshotName = getSlotScreenshotName(game, index)
        val file = getPreviewFile(screenshotName, coreID.coreName)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        ThumbnailUtils.extractThumbnail(bitmap, size, size)
    }

    fun setPreviewForSlot(
        game: Game,
        bitmap: Bitmap,
        coreID: CoreID,
        index: Int
    ) = Completable.fromAction {
        val screenshotName = getSlotScreenshotName(game, index)
        val file = getPreviewFile(screenshotName, coreID.coreName)
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
    }

    private fun getPreviewFile(fileName: String, coreName: String): File {
        val statesDirectories = File(directoriesManager.getStatesPreviewDirectory(), coreName)
        statesDirectories.mkdirs()
        return File(statesDirectories, fileName)
    }

    private fun getSlotScreenshotName(
        game: Game,
        index: Int
    ) = "${game.fileName}.slot${index + 1}.jpg"

    companion object {
        val PREVIEW_SIZE_DP = 96f
    }
}
