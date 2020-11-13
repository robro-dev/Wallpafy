package com.robro.wallpafy.util.adaptater

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.robro.wallpafy.R
import com.robro.wallpafy.layout.CoverPreviewButton
import com.robro.wallpafy.platform.spotify.data.Album

/**
 * Displays an album list with a [ImageButton] for each album.
 * The image is the cover of the album and the button display a menu to let the user whether set the
 * album cover as wallpaper or delete the album of the list
 *
 * @param albumList list of [Album] to display
 * @param previewSize size of recycle view
 * @param context context of the application
 * @param useCoverCallback method called when user wants to use the cover album as wallpaper. Takes the chosen album as parameter
 * @param deleteCoverCallback method called when the user wants to delete a album from the list
 */
class PreviewAdapter(
    private val albumList: MutableList<Album>,
    private val previewSize: Int,
    private val context: Context,
    private val useCoverCallback: (Album) -> Unit = {},
    private val deleteCoverCallback: () -> Unit = {}
) : RecyclerView.Adapter<PreviewAdapter.MyViewHolder>() {

    companion object {
        private const val emptyAlbumCover = R.drawable.cover_placeholder
    }

    class MyViewHolder(val imageButton: ImageButton) : RecyclerView.ViewHolder(imageButton)

    private val albumPlaceholder = getAlbumPlaceholder()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val imageButton = LayoutInflater.from(parent.context)
            .inflate(R.layout.cover_preview_button, parent, false) as CoverPreviewButton
        return MyViewHolder(imageButton)
    }

    override fun getItemCount() = albumList.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val album = albumList[position]

        Glide.with(context)
            .asBitmap()
            .apply(RequestOptions.overrideOf(previewSize))
            .load(album.getCoverURL(previewSize))
            .placeholder(albumPlaceholder)
            .into(holder.imageButton)

        holder.imageButton.setOnClickListener { button ->
            PopupMenu(context, button).also {
                it.menuInflater.inflate(R.menu.cover_menu, it.menu)
                it.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.use_cover -> {
                            useCoverCallback(album)
                        }

                        R.id.delete_cover -> {
                            albumList.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, itemCount)
                            deleteCoverCallback()
                        }
                    }

                    true
                }
                it.show()
            }
        }
    }

    private fun getAlbumPlaceholder(): Drawable {
        val bitmap = BitmapFactory.decodeResource(context.resources, emptyAlbumCover)
        return if (previewSize != 0) {
            val bitmapResized = Bitmap.createScaledBitmap(bitmap, previewSize, previewSize, false)
            BitmapDrawable(context.resources, bitmapResized)
        } else
            BitmapDrawable(context.resources, bitmap)
    }

}