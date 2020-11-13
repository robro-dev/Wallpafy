package com.robro.wallpafy.platform.spotify.data

/**
 * Contains data relative to a track list
 *
 * @property items list of tracks. Can be partial. Used to get the album thanks to [GetItem] and [GetItem.GetTrack]
 * @property next url of the rest of the list, if it is partial
 */
data class TrackList(
    val items: Array<GetItem>,
    val next: String?
) {
    data class GetItem(
        val track: GetTrack
    ) {
        data class GetTrack(
            val album: Album
        )
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}