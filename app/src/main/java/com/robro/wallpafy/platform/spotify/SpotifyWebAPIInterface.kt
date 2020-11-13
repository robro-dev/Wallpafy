package com.robro.wallpafy.platform.spotify

import com.robro.wallpafy.platform.spotify.data.TrackList
import com.robro.wallpafy.platform.spotify.data.User
import com.robro.wallpafy.platform.spotify.data.UserPlaylists
import retrofit2.Call
import retrofit2.http.*

/**
 * Binds functions to Spotify endpoints for retrofit2
 */
interface SpotifyWebAPIInterface {
    companion object {
        // Request only needed fields
        private const val fields = "items.track.album(id,album_type,images),next"
    }

    @GET("me")
    fun getUserInfo(): Call<User>

    @GET("me/player/recently-played")
    fun getRecentlyPlayedInfo(@Query("limit") limit: Int = 20): Call<TrackList>

    @GET("me/playlists")
    fun getCurentUserPlaylistsInfo(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Call<UserPlaylists>

    @GET("playlists/{playlist_id}/tracks?fields=$fields")
    fun getPlaylistTracksInfo(
        @Path("playlist_id") playlistId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("market") market: String = "from_token"
    ): Call<TrackList>

    @GET
    fun getTrackListFromURL(@Url url: String): Call<TrackList>

    @GET
    fun getPlaylistsFromURL(@Url url: String): Call<UserPlaylists>


}