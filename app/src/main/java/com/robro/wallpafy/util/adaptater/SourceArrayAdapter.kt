package com.robro.wallpafy.util.adaptater

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.robro.wallpafy.util.SourceEnum

/**
 * Displays the list available [SourceEnum] for retrieving user's album list
 */
class SourceArrayAdapter(context: Context, data: Array<SourceEnum>) :
    ArrayAdapter<SourceEnum>(context, android.R.layout.simple_list_item_1, data) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).also {
            val textView = it as TextView
            textView.text = context.resources.getString(getItem(position)!!.displayedNameResource)
        }
    }

    override fun isEnabled(position: Int): Boolean {
        return position != SourceEnum.CUSTOM.ordinal
    }

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getDropDownView(position, convertView, parent).also {
            val textView = it as TextView
            textView.text = context.resources.getString(getItem(position)!!.displayedNameResource)
            if (position == SourceEnum.CUSTOM.ordinal)
                textView.setTextColor(
                    ColorUtils.blendARGB(
                        textView.currentTextColor,
                        Color.BLACK,
                        0.2f
                    )
                )
        }
    }
}