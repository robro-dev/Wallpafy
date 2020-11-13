package com.robro.wallpafy.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.robro.wallpafy.MainActivity
import com.robro.wallpafy.R
import com.robro.wallpafy.platform.PlatformEnum

/**
 * Provides buttons to log in and log out to platforms
 */
class AccountsFragment : Fragment() {
    private lateinit var parentActivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parentActivity = activity as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.layout_fragment_accounts, container, false)
        val platformManager = parentActivity.platformManager

        // Init Spotify Button
        val spotifyButton = mainView.findViewById<AppCompatButton>(R.id.spotify_button)
        spotifyButton.setOnClickListener {
            if (platformManager.currentPlatform != PlatformEnum.SPOTIFY)
                platformManager.startLoginActivity(PlatformEnum.SPOTIFY, parentActivity)
            else
                createLogOutDialog().show()
        }

        when (platformManager.currentPlatform) {
            PlatformEnum.SPOTIFY -> if (platformManager.isReady())
                spotifyButton.text = platformManager.currentUser.name
            PlatformEnum.LOG_OUT -> spotifyButton.setText(R.string.spotify_button)
        }

        return mainView
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        (context as? MainActivity)?.setAccountsFragment(this)
    }

    override fun onResume() {
        super.onResume()
        onAccountChanged()
    }

    private fun createLogOutDialog(): AlertDialog {
        return AlertDialog.Builder(parentActivity)
            .setMessage(R.string.log_out_dialog_message)
            .setTitle(R.string.log_out_dialog_title)
            .setPositiveButton(R.string.log_out_dialog_positive) { _, _ ->
                parentActivity.platformManager.logOut(parentActivity)
            }
            .setNegativeButton(R.string.log_out_dialog_negative) { dialog, _ ->
                dialog.cancel()
            }.create()
    }

    fun onAccountChanged() {
        val platformManager = parentActivity.platformManager
        val spotifyButton = parentActivity.findViewById<AppCompatButton>(R.id.spotify_button)

        when (platformManager.currentPlatform) {
            PlatformEnum.SPOTIFY -> if (platformManager.isReady())
                spotifyButton.text = platformManager.currentUser.name
            PlatformEnum.LOG_OUT -> {
                spotifyButton.setText(R.string.spotify_button)
                // Remove the back navigation button if the user is not log in to a platform
                parentActivity.findViewById<Toolbar>(R.id.activity_main_toolbar).navigationIcon =
                    null
            }
        }
    }
}
