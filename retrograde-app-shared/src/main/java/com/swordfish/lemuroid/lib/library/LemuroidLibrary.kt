/*
 * GameLibrary.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.library

import com.gojuno.koptional.None
import com.gojuno.koptional.Optional
import com.gojuno.koptional.Some
import com.swordfish.lemuroid.common.rx.toSingleAsOptional
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.DataFile
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.storage.BaseStorageFile
import com.swordfish.lemuroid.lib.storage.GroupedStorageFiles
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.lemuroid.lib.storage.StorageFile
import com.swordfish.lemuroid.lib.storage.StorageProvider
import com.swordfish.lemuroid.lib.storage.StorageProviderRegistry
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import dagger.Lazy

class LemuroidLibrary(
    private val retrogradedb: RetrogradeDatabase,
    private val providerProviderRegistry: Lazy<StorageProviderRegistry>,
    private val biosManager: BiosManager
) {
    fun indexLibrary(): Completable {
        val startedAtMs = System.currentTimeMillis()

        return Single
            .fromCallable { providerProviderRegistry.get() }
            .flatMapObservable { Observable.fromIterable(it.enabledProviders) }
            .concatMap { provider ->
                provider.listBaseStorageFiles()
                    .map { StorageFilesMerger.mergeDataFiles(provider, it) }
                    .flatMap { Observable.fromIterable(it) }
                    .flatMapSingle { retrieveGameForUri(it) }
                    .buffer(BUFFER_SIZE)
                    .doOnNext { pairs -> updateExistingGames(pairs, startedAtMs) }
                    .doOnNext { pairs -> refreshGamesDataFiles(pairs, startedAtMs) }
                    .map { pairs -> filterNotExisting(pairs) }
                    .flatMap { retrieveGames(it, provider, startedAtMs) }
                    .buffer(BUFFER_SIZE)
                    .doOnNext { pairs ->

                        val games = pairs
                            .filter { (_, game) -> game is Some }
                            .map { (files, game) -> files to game.toNullable()!! }

                        val unknownFiles = pairs
                            .filter { (_, game) -> game is None }
                            .flatMap { (files, _) -> files.allFiles() }

                        handleNewGames(games, startedAtMs)
                        handleUnknownFiles(provider, unknownFiles, startedAtMs)
                    }
            }
            .doOnComplete { removeDeletedBios(startedAtMs) }
            .doOnComplete { removeDeletedGames(startedAtMs) }
            .doOnComplete { removeDeletedDataFiles(startedAtMs) }
            .doOnComplete {
                Timber.i(
                    "Library indexing completed in: ${System.currentTimeMillis() - startedAtMs} ms"
                )
            }
            .ignoreElements()
    }

    private fun removeDeletedBios(startedAtMs: Long) {
        biosManager.deleteBiosBefore(startedAtMs)
    }

    private fun handleNewGames(pairs: List<Pair<GroupedStorageFiles, Game>>, startedAtMs: Long) {
        val games = pairs.map { (_, game) -> game }
        games.forEach { Timber.d("Insert: $it") }

        val gameIds = retrogradedb.gameDao().insert(games)
        val dataFiles = pairs
            .map { it.first.dataFiles }
            .zip(gameIds)
            .flatMap { (files, gameId) ->
                files.map {
                    convertIntoDataFile(gameId.toInt(), it, startedAtMs)
                }
            }

        retrogradedb.dataFileDao().insert(dataFiles)
    }

    private fun convertIntoDataFile(
        gameId: Int,
        baseStorageFile: BaseStorageFile,
        startedAtMs: Long
    ): DataFile {
        return DataFile(
            gameId = gameId,
            fileUri = baseStorageFile.uri.toString(),
            fileName = baseStorageFile.name,
            lastIndexedAt = startedAtMs,
            path = baseStorageFile.path
        )
    }

    private fun handleUnknownFiles(
        provider: StorageProvider,
        files: List<BaseStorageFile>,
        startedAtMs: Long
    ) {
        files.forEach { baseStorageFile ->
            val storageFile = runCatching { provider.getStorageFile(baseStorageFile) }.getOrNull()
            val inputStream = storageFile?.uri?.let { provider.getInputStream(it) }

            if (storageFile != null && inputStream != null) {
                biosManager.tryAddBiosAfter(storageFile, inputStream, startedAtMs)
            }
        }
    }

    private fun retrieveGames(
        it: List<GroupedStorageFiles>,
        provider: StorageProvider,
        startedAtMs: Long
    ): Observable<Pair<GroupedStorageFiles, Optional<Game>>> {
        return Observable.fromIterable(it).flatMapSingle { storageFile ->
            retrieveGame(storageFile, provider, startedAtMs)
        }
    }

    private fun retrieveGame(
        groupedStorageFile: GroupedStorageFiles,
        provider: StorageProvider,
        startedAtMs: Long
    ): Single<Pair<GroupedStorageFiles, Optional<Game>>> {
        return Observable.fromIterable(sortedFilesForScanning(groupedStorageFile))
            .flatMapMaybe {
                Maybe.fromCallable<StorageFile> { provider.getStorageFile(it) }.onErrorComplete()
            }
            .flatMapSingle { storageFile ->
                provider.metadataProvider.retrieveMetadata(storageFile).map { storageFile to it }
            }
            .map { (storageFile, metadata) ->
                convertGameMetadataToGame(groupedStorageFile, storageFile, metadata, startedAtMs)
            }
            .filter { it is Some }
            .first(None)
            .map { groupedStorageFile to it }
    }

    private fun sortedFilesForScanning(groupedStorageFile: GroupedStorageFiles): List<BaseStorageFile> {
        return groupedStorageFile.dataFiles.sortedBy { it.name } + listOf(groupedStorageFile.primaryFile)
    }

    private fun convertGameMetadataToGame(
        groupedStorageFile: GroupedStorageFiles,
        storageFile: StorageFile,
        gameMetadataOptional: Optional<GameMetadata>,
        lastIndexedAt: Long
    ): Optional<Game> {

        if (gameMetadataOptional is None) return None
        val gameMetadata = gameMetadataOptional.component1()!!

        val gameSystem = GameSystem.findById(gameMetadata.system!!)

        // If the databased matched a data file (as with bin/cue) we force link the primary filename
        val fileName = if (groupedStorageFile.dataFiles.isNotEmpty()) {
            groupedStorageFile.primaryFile.name
        } else {
            storageFile.name
        }

        val game = Game(
            fileName = fileName,
            fileUri = groupedStorageFile.primaryFile.uri.toString(),
            title = gameMetadata.name ?: groupedStorageFile.primaryFile.name,
            systemId = gameSystem.id.dbname,
            developer = gameMetadata.developer,
            coverFrontUrl = gameMetadata.thumbnail,
            lastIndexedAt = lastIndexedAt
        )
        return Some(game)
    }

    private fun updateExistingGames(
        pairs: MutableList<Pair<GroupedStorageFiles, Optional<Game>>>,
        startedAtMs: Long
    ) {
        pairs.forEach { (storageFiles, game) ->
            val fileName = storageFiles.primaryFile.name
            Timber.d("Game already indexed? $fileName ${game is Some}")
        }

        val updatedGames = pairs.filter { (_, game) -> game is Some }
            .map { (_, game) -> game.component1()!!.copy(lastIndexedAt = startedAtMs) }

        updatedGames.forEach { Timber.d("Updating game: $it") }
        retrogradedb.gameDao().update(updatedGames)
    }

    private fun refreshGamesDataFiles(
        pairs: MutableList<Pair<GroupedStorageFiles, Optional<Game>>>,
        startedAtMs: Long
    ) {
        val dataFiles = pairs.filter { (_, game) -> game is Some }
            .flatMap { (storageFile, game) ->
                val gameId = game.component1()!!.id
                storageFile.dataFiles.map { convertIntoDataFile(gameId, it, startedAtMs) }
            }

        dataFiles.forEach { Timber.d("Adding new data file: $it") }
        retrogradedb.dataFileDao().insert(dataFiles)
    }

    private fun filterNotExisting(
        pairs: List<Pair<GroupedStorageFiles, Optional<Game>>>
    ): List<GroupedStorageFiles> {
        return pairs.filter { (_, game) -> game is None }
            .map { (storageFile, _) -> storageFile }
    }

    private fun retrieveGameForUri(
        storageFile: GroupedStorageFiles
    ): Single<Pair<GroupedStorageFiles, Optional<Game>>> {
        Timber.d("Retrieving game for uri: ${storageFile.primaryFile}")
        return retrogradedb.gameDao().selectByFileUri(storageFile.primaryFile.uri.toString())
            .toSingleAsOptional()
            .map { game -> storageFile to game }
    }

    private fun removeDeletedDataFiles(startedAtMs: Long) {
        val dataFiles = retrogradedb.dataFileDao().selectByLastIndexedAtLessThan(startedAtMs)
        retrogradedb.dataFileDao().delete(dataFiles)
    }

    private fun removeDeletedGames(startedAtMs: Long) {
        val games = retrogradedb.gameDao().selectByLastIndexedAtLessThan(startedAtMs)
        retrogradedb.gameDao().delete(games)
    }

    fun getGameFiles(
        game: Game,
        dataFiles: List<DataFile>,
        allowVirtualFiles: Boolean
    ): Single<RomFiles> {
        return Single.fromCallable { providerProviderRegistry.get() }
            .flatMap { it.getProvider(game).getGameRomFiles(game, dataFiles, allowVirtualFiles) }
    }

    companion object {
        // We batch database updates to avoid unnecessary UI updates.
        const val BUFFER_SIZE = 100
    }
}
