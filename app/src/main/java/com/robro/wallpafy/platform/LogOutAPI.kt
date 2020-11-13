package com.robro.wallpafy.platform

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.robro.wallpafy.R
import com.robro.wallpafy.platform.spotify.data.Album
import com.robro.wallpafy.platform.spotify.data.Playlist
import com.robro.wallpafy.platform.spotify.data.User
import net.openid.appauth.AuthorizationServiceConfiguration

/**
 * Handles API call when the user is logged out
 *
 * All API call display a toast to notify the user to log in before using the app
 * Normally, no function of this class should be call
 */
class LogOutAPI(private val context: Context) : PlatformAPI {
    override val serviceConfig: AuthorizationServiceConfiguration =
        AuthorizationServiceConfiguration(Uri.EMPTY, Uri.EMPTY)

    override fun startLoginActivity(parentActivity: AppCompatActivity) {
        Toast.makeText(context, R.string.log_out_toast, Toast.LENGTH_SHORT).show()
    }

    override fun getUserInformation(
        accessToken: String,
        successCallback: (userInformation: User) -> Unit,
        errorCallback: () -> Unit
    ) {
        Toast.makeText(context, R.string.log_out_toast, Toast.LENGTH_SHORT).show()
    }

    override fun getUserPaylists(
        accessToken: String,
        successCallback: (userPlaylists: List<Playlist>) -> Unit,
        errorCallback: () -> Unit
    ) {
        Toast.makeText(context, R.string.log_out_toast, Toast.LENGTH_SHORT).show()
    }

    override fun getRecentlyPlayed(
        accessToken: String,
        ignoreCompilations: Boolean,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        Toast.makeText(context, R.string.log_out_toast, Toast.LENGTH_SHORT).show()
    }

    override fun getPlaylistTracks(
        accessToken: String,
        ignoreCompilations: Boolean,
        playlistID: String,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        Toast.makeText(context, R.string.log_out_toast, Toast.LENGTH_SHORT).show()
    }
}