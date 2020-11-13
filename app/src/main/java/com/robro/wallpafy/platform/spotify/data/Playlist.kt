package com.robro.wallpafy.platform.spotify.data

/**
 * Contains data relative to a playlist
 *
 * @property id Spotify ID to the playlist
 * @property name name of the playlist
 * @property RECENTLY_PLAYED_PLAYLIST playlist used when user select recently played source
 * @property RECENTLY_PLAYED_ID ID  of the recently played playlist
 * @property CUSTOM_PLAYLIST playlist used when user modify a playlist
 * @property CUSTOM_ID ID of the custom playlist
 */
data class Playlist(
    val id: String,
    val name: String
) {
    companion object {
        const val RECENTLY_PLAYED_ID = "recently_played"
        const val CUSTOM_ID = "custom"

        val RECENTLY_PLAYED_PLAYLIST = Playlist(RECENTLY_PLAYED_ID, "")
        val CUSTOM_PLAYLIST = Playlist(CUSTOM_ID, "")
    }

    override fun toString(): String {
        return name
    }
}