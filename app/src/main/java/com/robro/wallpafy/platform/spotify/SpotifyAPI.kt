package com.robro.wallpafy.platform.spotify

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.robro.wallpafy.platform.PlatformAPI
import com.robro.wallpafy.platform.PlatformsID
import com.robro.wallpafy.platform.spotify.SpotifyAPI.Companion.RC_AUTH
import com.robro.wallpafy.platform.spotify.data.*
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Handles all calls to Spotify API
 *
 * @property serviceConfig Service configuration for authentication with AppAuth
 * @property RC_AUTH Request code used for a Spotify authentication
 */
class SpotifyAPI(private val context: Context) : PlatformAPI {
    companion object {
        private const val TAG = "SpotifyAPI"

        private const val REDIRECT_URI = "com.robro.wallpafy://callback"
        private const val CLIENT_ID = PlatformsID.SPOTIFY_CLIENT_ID
        private const val SCOPE =
            "user-read-recently-played playlist-read-private playlist-read-collaborative"
        private const val WEB_API_URL = "https://api.spotify.com/v1/"
        private const val AUTH_URL = "https://accounts.spotify.com/authorize"
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        const val RC_AUTH = 1302
    }

    override val serviceConfig = AuthorizationServiceConfiguration(
        Uri.parse(AUTH_URL),
        Uri.parse(TOKEN_URL)
    )

    private val authRequest = AuthorizationRequest.Builder(
        serviceConfig,
        CLIENT_ID,
        ResponseTypeValues.CODE,
        Uri.parse(REDIRECT_URI)
    ).setScope(SCOPE)
        .build()

    /**
     * Starts AppAuth login activity for Spotify to get an OAuth authorization code
     *
     * @param parentActivity Activity that will receive the result of the attempt
     */
    override fun startLoginActivity(parentActivity: AppCompatActivity) {
        val authService = AuthorizationService(context)
        val authIntent = authService.getAuthorizationRequestIntent(authRequest)

        parentActivity.startActivityForResult(
            authIntent,
            RC_AUTH
        )
    }

    /**
     * Calls the [successCallback] with the [User] requested from Spotify
     *
     * @param accessToken OAuth token for the request
     * @param successCallback function to call with the received [User]
     * @param errorCallback function to call if the request failed
     */
    override fun getUserInformation(
        accessToken: String,
        successCallback: (userInformation: User) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getUserInfo()

        callJson.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>) {
                response.body()?.let { successCallback(it) }
            }

            override fun onFailure(call: Call<User>, t: Throwable) {
                Log.e(TAG, "getUserInformation_failure", t)
                errorCallback()
            }
        })

    }

    /**
     * Calls the [successCallback] with the list of user's [Playlist] requested from Spotify
     *
     * @param accessToken OAuth token for the request
     * @param successCallback function to call with the received list of [Playlist]
     * @param errorCallback function to call if the request failed
     */
    override fun getUserPaylists(
        accessToken: String,
        successCallback: (userPlaylists: List<Playlist>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getCurentUserPlaylistsInfo(50, 0)

        callJson.enqueue(object : Callback<UserPlaylists> {
            override fun onResponse(call: Call<UserPlaylists>, response: Response<UserPlaylists>) {
                response.body()?.let {

                    // Retrieve the rest of the playlist list if next url provided
                    if (!it.next.isNullOrEmpty())
                        getUserPlaylistsFromURL(
                            accessToken,
                            it.next,
                            it.items,
                            successCallback,
                            errorCallback
                        )
                    else // If no next url provided the list is complete
                        successCallback(it.items)
                }
            }

            override fun onFailure(call: Call<UserPlaylists>, t: Throwable) {
                Log.e(TAG, "getUserPlaylists_failure", t)
                errorCallback()
            }
        })
    }

    /**
     * Calls the [successCallback] with the list of user's recently played [Album] requested from Spotify
     *
     * @param accessToken OAuth token for the request
     * @param successCallback function to call with the received list of [Album]
     * @param errorCallback function to call if the request failed
     */
    override fun getRecentlyPlayed(
        accessToken: String,
        ignoreCompilations: Boolean,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getRecentlyPlayedInfo()

        callJson.enqueue(object : Callback<TrackList> {
            override fun onResponse(call: Call<TrackList>, response: Response<TrackList>) {
                response.body()?.let {
                    val albumList =
                        it.items.fold(listOf()) { acc: List<Album>, item: TrackList.GetItem ->
                            val album = item.track.album
                            if (!acc.contains(album) && (!ignoreCompilations || album.albumType != Album.COMPILATION))
                                acc + album
                            else
                                acc
                        }

                    // Retrieve the rest of the album list if next url provided
                    if (!it.next.isNullOrEmpty())
                        getTrackListFromURL(
                            accessToken,
                            it.next,
                            ignoreCompilations,
                            albumList,
                            successCallback,
                            errorCallback
                        )
                    else // If no next url provided the list is complete
                        successCallback(albumList)
                }
            }

            override fun onFailure(call: Call<TrackList>, t: Throwable) {
                Log.e(TAG, "getRecentlyPlayed_failure", t)
                errorCallback()
            }
        })
    }

    /**
     * Calls the [successCallback] with the [Album] list of a playlist requested from Spotify
     *
     * @param accessToken OAuth token for the request
     * @param ignoreCompilations if true, compilations will not be added to the [Album] list
     * @param playlistID Spotify ID of the playlist to get the album list
     * @param successCallback function to call with the received list of [Album]
     * @param errorCallback function to call if the request failed
     */
    override fun getPlaylistTracks(
        accessToken: String,
        ignoreCompilations: Boolean,
        playlistID: String,
        successCallback: (albumList: List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getPlaylistTracksInfo(playlistID)

        callJson.enqueue(object : Callback<TrackList> {
            override fun onResponse(call: Call<TrackList>, response: Response<TrackList>) {
                response.body()?.let {
                    val albumList =
                        it.items.fold(listOf()) { acc: List<Album>, item: TrackList.GetItem ->
                            val album = item.track.album
                            if (!acc.contains(album) && (!ignoreCompilations || album.albumType != Album.COMPILATION))
                                acc + album
                            else
                                acc
                        }

                    // Retrieve the rest of the album list if next url provided
                    if (!it.next.isNullOrEmpty())
                        getTrackListFromURL(
                            accessToken,
                            it.next,
                            ignoreCompilations,
                            albumList,
                            successCallback,
                            errorCallback
                        )
                    else // If no next url provided the list is complete
                        successCallback(albumList)
                }
            }

            override fun onFailure(call: Call<TrackList>, t: Throwable) {
                Log.e(TAG, "getRecentlyPlayed_failure", t)
                errorCallback()
            }
        })
    }

    /**
     * Builds a retrofit service to Spotify API with OAuth token
     */
    private fun buildRetrofitService(accessToken: String): SpotifyWebAPIInterface {
        val interceptor = Interceptor { chain ->
            val newRequest: Request =
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $accessToken").build()
            chain.proceed(newRequest)
        }

        val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

        return Retrofit.Builder()
            .baseUrl(WEB_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SpotifyWebAPIInterface::class.java)
    }

    /**
     * Same as [getUserPaylists] but from URL provided by Spotify
     * Used to retrieve the rest of a list
     */
    private fun getUserPlaylistsFromURL(
        accessToken: String,
        url: String,
        accUserPlaylists: List<Playlist>,
        successCallback: (userPlaylists: List<Playlist>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getPlaylistsFromURL(url)

        callJson.enqueue(object : Callback<UserPlaylists> {
            override fun onResponse(call: Call<UserPlaylists>, response: Response<UserPlaylists>) {
                response.body()?.let {

                    // Retrieve the rest of the album list if next url provided
                    if (!it.next.isNullOrEmpty())
                        getUserPlaylistsFromURL(
                            accessToken,
                            it.next,
                            accUserPlaylists + it.items,
                            successCallback,
                            errorCallback
                        )
                    else // If no next url provided the list is complete
                        successCallback(accUserPlaylists + it.items)

                }
            }

            override fun onFailure(call: Call<UserPlaylists>, t: Throwable) {
                Log.e(TAG, "getUserPlaylistsFromURL_failure", t)
                errorCallback()
            }

        })
    }

    /**
     * Same as [getPlaylistTracks] or [getRecentlyPlayed] but from URL provided by Spotify
     * Used to retrieve the rest of a list
     */
    private fun getTrackListFromURL(
        accessToken: String,
        url: String,
        hideCompilations: Boolean,
        accTrackList: List<Album>,
        successCallback: (recentlyPlayed: List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        val callJson = buildRetrofitService(accessToken).getTrackListFromURL(url)

        callJson.enqueue(object : Callback<TrackList> {
            override fun onResponse(call: Call<TrackList>, response: Response<TrackList>) {
                response.body()?.let {
                    val albumList =
                        it.items.fold(accTrackList) { acc: List<Album>, item: TrackList.GetItem ->
                            val album = item.track.album
                            if (!acc.contains(album) && (!hideCompilations || album.albumType != Album.COMPILATION))
                                acc + album
                            else
                                acc
                        }

                    // Retrieve the rest of the album list if next url provided
                    if (!it.next.isNullOrEmpty())
                        getTrackListFromURL(
                            accessToken,
                            it.next,
                            hideCompilations,
                            albumList,
                            successCallback,
                            errorCallback
                        )
                    else // If no next url provided the list is complete
                        successCallback(albumList)
                }
            }

            override fun onFailure(call: Call<TrackList>, t: Throwable) {
                Log.e(TAG, "getRecentlyPlayed_failure", t)
                errorCallback()
            }
        })
    }
}