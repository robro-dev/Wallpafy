package com.robro.wallpafy.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.robro.wallpafy.R

/**
 * Provides all settings of the application
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.fragment_settings, rootKey)
    }
}
