package com.robro.wallpafy

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.robro.wallpafy.platform.spotify.data.Image
import com.robro.wallpafy.platform.spotify.data.Playlist.Companion.CUSTOM_ID
import kotlinx.coroutines.Runnable
import java.time.Duration
import kotlin.math.roundToInt

/**
 * Shows the live wallpaper of Wallpafy if the user set a list of cover as wallpaper
 * Gets a list of wallpaper from the shared preferences and displays them periodically
 *
 * Creates a [CoverUpdateWorker] to update the list of wallpaper according to the playlist the user has chosen
 * This worker is canceled when the service in unbind
 *
 * Wallpapers to displayed is received from the shared preference under a JSON object containing
 * the date of the last update in [System.currentTimeMillis] format and a list of URL representing wallpapers
 *
 * @property PLAYLIST_ID_KEY key of the playlist ID to display in the shared preferences
 * @property WALLPAPER_LIST key of the wallpaper list in the shared preferences
 * @property LAST_UPDATE_JSON key of the json value containing the last update of the wallpaper list
 * @property COVER_ARRAY_JSON key of the json value containing the array of the wallpaper to display
 */
class WallpafyService : WallpaperService() {
    companion object {
        const val WALLPAPER_LIST = "wallpaper_list"
        const val LAST_UPDATE_JSON = "last_update"
        const val COVER_ARRAY_JSON = "cover_array"
        const val PLAYLIST_ID_KEY = "playlist_id"

        private const val DEFAULT_FREQUENCY = "3600"

        private const val NULL = ""
    }

    private val context: Context = this
    private lateinit var engine: WallpafyEngine

    /**
     * Implementation of the live wallpaper
     */
    inner class WallpafyEngine : Engine() {
        private val handler = Handler()

        private var surfaceHeight = surfaceHolder.surfaceFrame.height()
        private var surfaceWidth = surfaceHolder.surfaceFrame.width()
        private var coverDisplayWidth = 0
        private var xOffset = 0f
        private var offsetMax = 0

        private var coverList = listOf<Image>()
        private var lastCover = Image.EMPTY
        private var lastUpdateTime = 0L

        private lateinit var currentWallpaper: Bitmap

        private val updateRunner = Runnable {
            updateCover()
        }

        override fun onCreate(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            handler.post(updateRunner)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(updateRunner)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder?,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)
            this.surfaceWidth = width
            this.surfaceHeight = height
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            super.onOffsetsChanged(
                xOffset,
                yOffset,
                xOffsetStep,
                yOffsetStep,
                xPixelOffset,
                yPixelOffset
            )
            this.xOffset = xOffset
            draw()
        }

        /**
         * Returns a cover randomly chosen among the list of wallpaper.
         * Updates the list before if shared preferences list has been updated
         */
        private fun getCover(): Image {
            val prefJson = JsonParser().parse(
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getString(WALLPAPER_LIST, "{}")
            ).asJsonObject

            if (!prefJson.has(LAST_UPDATE_JSON) || !prefJson.has(COVER_ARRAY_JSON)) return Image.EMPTY

            // Update cover list if needed
            val updateTime = prefJson.get(LAST_UPDATE_JSON).asLong
            if (lastUpdateTime < updateTime) {


                val type = object : TypeToken<List<Image>>() {}.type
                coverList = Gson().fromJson(prefJson.get(COVER_ARRAY_JSON), type)

                lastUpdateTime = updateTime
            }

            var currentCover = coverList.shuffled().first()

            while (currentCover == lastCover && coverList.size > 1)
                currentCover = coverList.shuffled().first()

            return currentCover
        }

        /**
         * Retrieves the frequency of the wallpaper change in shared preferences
         */
        private fun getWallpaperFrequency(): Long {
            val frequencyPref = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.wallpaper_frequency_key), DEFAULT_FREQUENCY)
                ?: DEFAULT_FREQUENCY

            return frequencyPref.toLong() * 1000L
        }

        /**
         * Download the cover from its URL and redraw the wallpaper with the new image
         */
        fun updateCover() {
            val cover = getCover()

            if (cover != Image.EMPTY) {
                val width = cover.width
                val screenRatio = surfaceWidth.toFloat() / surfaceHeight.toFloat()
                coverDisplayWidth = (width * screenRatio).roundToInt()
                val offset = (width - coverDisplayWidth) / 2
                Glide.with(context)
                    .asBitmap()
                    .load(cover.url)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            val wallpaper =
                                Bitmap.createBitmap(
                                    resource,
                                    offset,
                                    0,
                                    resource.width - offset,
                                    resource.height
                                )
                            currentWallpaper = wallpaper
                            offsetMax = currentWallpaper.width - coverDisplayWidth
                            draw()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }

            handler.removeCallbacks(updateRunner)
            handler.postDelayed(updateRunner, getWallpaperFrequency())
        }

        /**
         * Draw an image as wallpaper
         */
        private fun draw() {
            if (this::currentWallpaper.isInitialized) {
                val holder = surfaceHolder
                val canvas = holder.lockCanvas()

                val offset = (offsetMax * xOffset).roundToInt()
                canvas.drawBitmap(
                    currentWallpaper,
                    Rect(0 + offset, 0, coverDisplayWidth + offset, currentWallpaper.height),
                    Rect(0, 0, surfaceWidth, surfaceHeight),
                    null
                )
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    /**
     * Only use to update the wallpaper on running service
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (this::engine.isInitialized)
            engine.updateCover()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        startUpdateWorker()
    }

    override fun onCreateEngine(): Engine {
        engine = WallpafyEngine().apply {
            setTouchEventsEnabled(false)
        }

        return engine
    }

    /**
     * Used to cancel running [CoverUpdateWorker]
     */
    override fun onUnbind(intent: Intent?): Boolean {
        WorkManager.getInstance(context).cancelAllWorkByTag(CoverUpdateWorker.TAG)
        return super.onUnbind(intent)
    }

    /**
     * Starts the [CoverUpdateWorker] if the playlist chosen by the user is not custom
     * Retrieves the update frequency in shared preferences
     */
    private fun startUpdateWorker() {
        val prefManager = PreferenceManager.getDefaultSharedPreferences(context)
        val updateIntervalPref =
            prefManager.getString(context.getString(R.string.update_frequency_key), "0") ?: "0"
        val updateInterval = Duration.ofSeconds(updateIntervalPref.toLong())
        val playlistID = prefManager.getString(PLAYLIST_ID_KEY, NULL) ?: NULL
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        if (updateInterval != Duration.ZERO && playlistID != NULL && playlistID != CUSTOM_ID) {
            val workRequest =
                PeriodicWorkRequestBuilder<CoverUpdateWorker>(
                    updateInterval,
                    updateInterval.dividedBy(10)
                ).setConstraints(constraints)
                    .setInitialDelay(updateInterval)
                    .setInputData(workDataOf(CoverUpdateWorker.PLAYLIST_ID_KEY to playlistID))
                    .addTag(CoverUpdateWorker.TAG)
                    .build()

            WorkManager.getInstance(context).apply {
                cancelAllWorkByTag(CoverUpdateWorker.TAG)
                enqueue(workRequest)
            }
        }
    }
}