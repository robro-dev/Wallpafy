package com.robro.wallpafy.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import com.robro.wallpafy.R
import com.robro.wallpafy.BuildConfig

/**
 * Provides useful information about the application
 */
class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.layout_fragment_about, container, false)

        val versionSummary =  mainView.findViewById<AppCompatTextView>(R.id.version_summary)
        versionSummary.text = BuildConfig.VERSION_NAME

        return mainView
    }


}
