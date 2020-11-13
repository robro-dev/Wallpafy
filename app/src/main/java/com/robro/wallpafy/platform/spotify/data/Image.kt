package com.robro.wallpafy.platform.spotify.data

/**
 * Contains data relative to a image
 *
 * @property height supposed height of the image. Provided height can be inaccurate
 * @property width supposed width of the image. Provided width can be inaccurate
 * @property url url of the image
 * @property EMPTY provided an empty image
 */
data class Image(
    val height: Int,
    val width: Int,
    val url: String
) : Comparable<Image> {

    companion object {
        val EMPTY = Image(0, 0, "")
    }

    override fun compareTo(other: Image) = height - other.height

    override fun equals(other: Any?): Boolean {
        return if (other !is Image) false else url == other.url
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

}
