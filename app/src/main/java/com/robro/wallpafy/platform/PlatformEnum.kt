package com.robro.wallpafy.platform

import android.content.Context
import com.robro.wallpafy.R
import com.robro.wallpafy.platform.spotify.SpotifyAPI

/**
 * Contains all available platforms
 *
 * @property logoID resource ID of the platform logo
 * @property displayNameID resource ID of the platform name
 */
enum class PlatformEnum(val logoID: Int, val displayNameID: Int) {
    LOG_OUT(R.drawable.ic_highlight_off_red_600_18dp, R.string.log_out),
    SPOTIFY(R.drawable.ic_spotify_green_18dp, R.string.spotify);

    companion object {
        /**
         * Retrieves a platform by its name
         *
         * @param name [PlatformEnum.name] of the platform to get
         * @return a platform according to its name
         */
        fun getPlatformEnumFromName(name: String): PlatformEnum {
            return when (name) {
                LOG_OUT.name -> LOG_OUT
                SPOTIFY.name -> SPOTIFY
                else -> LOG_OUT
            }
        }
    }

    /**
     * Returns the API implementation of the platform
     *
     *  @param context context of the application
     *  @return the API implementation of the platform
     */
    fun getPlatformAPI(context: Context): PlatformAPI {
        return when (this) {
            SPOTIFY -> SpotifyAPI(context)
            else -> LogOutAPI(context)
        }
    }
}