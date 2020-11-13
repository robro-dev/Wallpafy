package com.robro.wallpafy.platform.spotify.data

import com.google.gson.annotations.SerializedName
import kotlin.math.abs

/**
 * Contains data relative to an album
 *
 * @property id Spotify ID of the album
 * @property albumType type of the album, used to ignore compilations
 * @property covers covers of the album, most of the time the same with different sizes
 * @property COMPILATION [albumType] of compilations
 */
data class Album(
    val id: String,
    @SerializedName("album_type") val albumType: String,
    @SerializedName("images") val covers: Array<Image>
) {

    companion object {
        const val COMPILATION = "compilation"
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is Album) false else id == other.id
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    /**
     * Get the url of the cover that match the most the the desired [height]
     */
    fun getCoverURL(height: Int): String {
        return covers.filter { it.height >= height }
            .minBy { abs(height - it.height) }
            ?.url ?: covers[0].url
    }

    /**
     * Get the cove with the greatest height
     */
    fun getLargestCover() : Image {
        return covers.toList().max() ?: Image.EMPTY
    }
}