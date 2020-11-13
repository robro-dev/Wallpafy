package com.robro.wallpafy

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.robro.wallpafy.platform.PlatformManager
import com.robro.wallpafy.platform.spotify.data.Album

/**
 * Updates the album list according to the playlist chosen by the user.
 * Retrieves the albums thanks to the [PlatformManager] and save it to the shared preferences so the [WallpafyService] can get the updated list
 *
 * @property TAG tag of the worker
 * @property PLAYLIST_ID_KEY key of the playlist ID to update in the inputData of the work
 */
class CoverUpdateWorker(context: Context, workerParam: WorkerParameters) :
    Worker(context, workerParam) {

    companion object {
        const val TAG = "WALLPAFY_UPDATE_WORKER"
        const val PLAYLIST_ID_KEY = "PLAYLIST_ID"
    }

    override fun doWork(): Result {
        val playlistIDInput = inputData.getString(PLAYLIST_ID_KEY) ?: return Result.failure()

        PlatformManager(applicationContext, false).apply {
            if (!loadState({ getAlbumList(playlistIDInput, { saveAlbum(it) }, {}) }))
                return Result.failure()
        }

        return Result.success()
    }

    private fun saveAlbum(albumList: List<Album>) {
        val json = JsonObject()

        val coverList = albumList.map { it.getLargestCover() }

        json.add(WallpafyService.COVER_ARRAY_JSON, Gson().toJsonTree(coverList))
        json.addProperty(WallpafyService.LAST_UPDATE_JSON, System.currentTimeMillis())

        PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .edit()
            .putString(WallpafyService.WALLPAPER_LIST, json.toString())
            .apply()
    }
}