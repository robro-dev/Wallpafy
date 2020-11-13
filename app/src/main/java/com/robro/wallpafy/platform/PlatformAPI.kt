package com.robro.wallpafy.platform

import androidx.appcompat.app.AppCompatActivity
import com.robro.wallpafy.platform.spotify.data.Album
import com.robro.wallpafy.platform.spotify.data.Playlist
import com.robro.wallpafy.platform.spotify.data.User
import net.openid.appauth.AuthorizationServiceConfiguration

/**
 * Provides a common interface for the API call of all platforms used in Wallpafy
 *
 * See implementation for more details
 *
 * @see com.robro.wallpafy.platform.spotify.SpotifyAPI
 * @see com.robro.wallpafy.platform.LogOutAPI
 */
interface PlatformAPI {
    val serviceConfig: AuthorizationServiceConfiguration

    /**
     * Start an activity to let the user log in to a platform, in order to get an OAuth authorization code
     */
    fun startLoginActivity(parentActivity: AppCompatActivity)

    /**
     * Get the current user information
     */
    fun getUserInformation(
        accessToken: String,
        successCallback: (userInformation: User) -> Unit,
        errorCallback: () -> Unit
    )

    /**
     * Get the a list of the playlist of the current user
     */
    fun getUserPaylists(
        accessToken: String,
        successCallback: (userPlaylists: List<Playlist>) -> Unit,
        errorCallback: () -> Unit
    )

    /**
     * Get the list of the recently played album of the current user
     */
    fun getRecentlyPlayed(
        accessToken: String,
        ignoreCompilations: Boolean,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    )

    /**
     * Get the album list of one playlist
     */
    fun getPlaylistTracks(
        accessToken: String,
        ignoreCompilations: Boolean,
        playlistID: String,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    )
}