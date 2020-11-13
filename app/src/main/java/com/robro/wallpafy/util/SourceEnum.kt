package com.robro.wallpafy.util

import com.robro.wallpafy.R

/**
 * Enumerates the source where to retrieve the album list of the user
 */
enum class SourceEnum(val displayedNameResource: Int) {
    RECENTLY_PLAYED(R.string.recently_played),
    PLAYLIST(R.string.playlist),
    CUSTOM(R.string.custom);
}