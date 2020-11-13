package com.robro.wallpafy.platform.spotify.data

/**
 * Contains data relative to the playlist of an user
 *
 * @property items list of the user's playlists. Can be partial
 * @property next url of the rest of the list, if it is partial
 */
data class UserPlaylists(
    val items: List<Playlist>,
    val next: String?
)