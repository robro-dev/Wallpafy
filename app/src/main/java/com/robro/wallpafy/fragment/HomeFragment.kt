package com.robro.wallpafy.fragment

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.FileProvider
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.robro.wallpafy.MainActivity
import com.robro.wallpafy.R
import com.robro.wallpafy.WallpafyService
import com.robro.wallpafy.platform.spotify.data.Album
import com.robro.wallpafy.platform.spotify.data.Playlist
import com.robro.wallpafy.platform.spotify.data.Playlist.Companion.CUSTOM_PLAYLIST
import com.robro.wallpafy.platform.spotify.data.Playlist.Companion.RECENTLY_PLAYED_ID
import com.robro.wallpafy.platform.spotify.data.Playlist.Companion.RECENTLY_PLAYED_PLAYLIST
import com.robro.wallpafy.util.SourceEnum
import com.robro.wallpafy.util.adaptater.PreviewAdapter
import com.robro.wallpafy.util.adaptater.SourceArrayAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Lets the user select which playlist or album used as Wallpaper
 */
class HomeFragment : Fragment() {
    private lateinit var parentActivity: MainActivity

    private lateinit var sourceSpinner: AppCompatSpinner
    private lateinit var sourceParameterSpinner: AppCompatSpinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ContentLoadingProgressBar
    private lateinit var confirmButton: AppCompatButton

    private val albumList = mutableListOf<Album>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.layout_fragment_home, container, false)
        val gridLayoutManager =
            GridLayoutManager(parentActivity, 2, LinearLayoutManager.HORIZONTAL, false)

        progressBar = mainView.findViewById(R.id.preview_progress_bar)
        progressBar.show()

        recyclerView = mainView.findViewById(R.id.preview_grid)
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() { // Wait until the view size is known before initializing the adapter view
                recyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val viewAdapter =
                    PreviewAdapter(
                        albumList,
                        recyclerView.height / 2,
                        parentActivity,
                        { album ->
                            setWallpaperFromUrl(album.getLargestCover().url)
                        }, {
                            disablePlaylistSpinner(CUSTOM_PLAYLIST)
                            sourceSpinner.setSelection(SourceEnum.CUSTOM.ordinal)
                        }
                    )

                recyclerView.apply {
                    setHasFixedSize(true)
                    layoutManager = gridLayoutManager
                    adapter = viewAdapter
                }
                initSpinners(viewAdapter)
            }
        })

        sourceSpinner = mainView.findViewById(R.id.source_spinner)
        sourceParameterSpinner = mainView.findViewById(R.id.source_parameter_spinner)

        confirmButton = mainView.findViewById(R.id.confirm_button)
        confirmButton.setOnClickListener {
            if (albumList.isNotEmpty()) {
                savePlaylistState()
                startOrUpdateLiveWallpaper()
            }
        }

        return mainView
    }

    override fun onResume() {
        super.onResume()
        val navController =
            (parentActivity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController

        if (!parentActivity.platformManager.isAuthorized()) {
            Toast.makeText(
                parentActivity,
                R.string.log_out_toast,
                Toast.LENGTH_SHORT
            ).show()
            navController.navigate(R.id.action_home_fragment_to_accounts_fragment)
        }
    }

    private fun disablePlaylistSpinner(playlist: Playlist) {
        sourceParameterSpinner.adapter = ArrayAdapter(
            parentActivity,
            android.R.layout.simple_list_item_1,
            arrayOf(playlist)
        )
        sourceParameterSpinner.isEnabled = false
    }

    private fun initSpinners(previewAdapter: PreviewAdapter) {
        val updateAlbumCallback = { newAlbumList: List<Album> ->
            albumList.clear()
            albumList.addAll(newAlbumList)
            previewAdapter.notifyDataSetChanged()
            progressBar.hide()

        }

        val platformAccessFailedCallback = {
            Toast.makeText(
                parentActivity,
                R.string.platform_access_failed_toast,
                Toast.LENGTH_SHORT
            ).show()
            progressBar.hide()
        }

        sourceSpinner.adapter = SourceArrayAdapter(parentActivity, SourceEnum.values())
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (parent.getItemAtPosition(position)) {
                    SourceEnum.PLAYLIST -> {
                        sourceParameterSpinner.isEnabled = true
                        parentActivity.platformManager.getUserPlaylists({
                            sourceParameterSpinner.adapter =
                                ArrayAdapter(
                                    parentActivity,
                                    android.R.layout.simple_list_item_1,
                                    it
                                )
                        }, platformAccessFailedCallback)
                    }
                    SourceEnum.RECENTLY_PLAYED -> {
                        disablePlaylistSpinner(RECENTLY_PLAYED_PLAYLIST)
                        progressBar.show()
                        parentActivity.platformManager.getAlbumList(
                            RECENTLY_PLAYED_ID,
                            updateAlbumCallback,
                            platformAccessFailedCallback
                        )
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        sourceParameterSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (parent.getItemAtPosition(position) != CUSTOM_PLAYLIST) {
                        progressBar.show()
                        parentActivity.platformManager.getAlbumList(
                            (parent.getItemAtPosition(position) as Playlist).id,
                            updateAlbumCallback,
                            platformAccessFailedCallback
                        )
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
    }

    private fun startOrUpdateLiveWallpaper() {
        val currentWallpaperService =
            WallpaperManager.getInstance(parentActivity).wallpaperInfo?.serviceName

        if (currentWallpaperService == WallpafyService::class.qualifiedName) {
            // Refresh current service if running
            parentActivity.startService(Intent(parentActivity, WallpafyService::class.java))
            Toast.makeText(
                parentActivity,
                R.string.wallpaper_list_updated_toast,
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Create service and set live wallpaper
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(parentActivity, WallpafyService::class.java)
            )
            startActivity(intent)
        }

    }

    private fun setWallpaperFromUrl(url: String) {
        Glide.with(parentActivity)
            .asBitmap()
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .load(url).into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    setWallpaperFromBitmap(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setWallpaperFromBitmap(wallpaper: Bitmap) {
        // Save the bitmap in the cache storage
        val currentWallpaperDirectory =
            File(parentActivity.cacheDir, "currentWallpaper")
        val currentWallpaperFile =
            File(currentWallpaperDirectory, "currentWallpaper.jpg")

        if (!currentWallpaperDirectory.exists())
            currentWallpaperDirectory.mkdir()

        try {
            val out = FileOutputStream(currentWallpaperFile)
            wallpaper.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: IOException) {
            Log.e("SetWallpaperFromUrl", "Failed to write cover on file system")
            Toast.makeText(parentActivity, R.string.write_cover_failed_toast, Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Get the content uri of the saved bitmap
        val contentUri = FileProvider.getUriForFile(
            parentActivity,
            parentActivity.packageName,
            currentWallpaperFile
        )

        // Launch system's wallpaper activity
        val intent =
            WallpaperManager.getInstance(parentActivity)
                .getCropAndSetWallpaperIntent(contentUri)
        parentActivity.startActivity(intent)
    }

    private fun savePlaylistState() {
        val json = JsonObject()

        val coverList = albumList.map { it.getLargestCover() }

        json.add(WallpafyService.COVER_ARRAY_JSON, Gson().toJsonTree(coverList))
        json.addProperty(WallpafyService.LAST_UPDATE_JSON, System.currentTimeMillis())

        PreferenceManager.getDefaultSharedPreferences(parentActivity)
            .edit()
            .putString(WallpafyService.WALLPAPER_LIST, json.toString())
            .putString(
                WallpafyService.PLAYLIST_ID_KEY,
                (sourceParameterSpinner.selectedItem as Playlist).id
            )
            .apply()
    }
}
