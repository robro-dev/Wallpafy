package com.robro.wallpafy.platform.spotify.data

import com.google.gson.annotations.SerializedName
import com.robro.wallpafy.platform.spotify.data.User.Companion.LOGGED_OUT

/**
 * Contains data relative to an user
 *
 * @property name username
 * @property images profile pictures of the user
 * @property LOGGED_OUT the logged out user. Used when the user is not log to any platform
 */
data class User(
    @SerializedName("display_name") val name: String,
    @SerializedName("images") val images: Array<Image>
) {
    companion object {
        val LOGGED_OUT = User("", arrayOf(Image.EMPTY))
    }

    /**
     * @return the url of the first image of the profile pictures
     */
    fun getProfilePictureURL(): String {
        return images.firstOrNull()?.url ?: ""
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (name != other.name) return false
        if (!images.contentEquals(other.images)) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * name.hashCode() + images.contentHashCode()
    }
}