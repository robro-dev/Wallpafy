package com.robro.wallpafy

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.robro.wallpafy.fragment.AccountsFragment
import com.robro.wallpafy.platform.PlatformManager
import com.robro.wallpafy.platform.spotify.SpotifyAPI
import com.robro.wallpafy.platform.spotify.data.User
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse

/**
 * Main activity of the application.
 *
 * Initializes the navigation controller, the toolbar and the [PlatformManager]
 * Received and send responses from the AppAuth login activity to the [PlatformManager]
 * Dispatches the updates relative to a change of user or platform
 */
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    lateinit var platformManager: PlatformManager

    private lateinit var accountsFragment: AccountsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_activity_main)
        setSupportActionBar(findViewById(R.id.activity_main_toolbar))

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        findViewById<NavigationView>(R.id.activity_main_nav_view)
            .setupWithNavController(navController)
        val appBarConfiguration = AppBarConfiguration(
            navController.graph, findViewById<DrawerLayout>(R.id.activity_main_drawer_layout)
        )
        findViewById<Toolbar>(R.id.activity_main_toolbar)
            .setupWithNavController(navController, appBarConfiguration)

        platformManager = PlatformManager(this, true)

        platformManager.loadState(
            { onUserChanged(it) },
            {
                navController.navigate(R.id.action_home_fragment_to_accounts_fragment)
                platformManager.logOut(this)
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Spotify connection request
        if (requestCode == SpotifyAPI.RC_AUTH) {
            if (intent != null) {

                val response = AuthorizationResponse.fromIntent(intent)
                val exception = AuthorizationException.fromIntent(intent)

                platformManager.requestToken(
                    response,
                    exception,
                    {
                        onUserChanged(it)
                        navController.navigate(R.id.action_accounts_fragment_to_home_fragment)
                    },
                    { }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        platformManager.saveState()
    }

    fun setAccountsFragment(accountsFragment: AccountsFragment) {
        this.accountsFragment = accountsFragment
    }

    fun onUserChanged(newUser: User) {
        val profilePicture = findViewById<AppCompatImageView>(R.id.profile_picture)
        val platformLogo = findViewById<AppCompatImageView>(R.id.platform_logo)
        val navigationHeaderView =
            findViewById<NavigationView>(R.id.activity_main_nav_view)
                .getHeaderView(0)
        val navHeaderLogo =
            navigationHeaderView.findViewById<AppCompatImageView>(R.id.nav_header_logo)
        val navHeaderUsername =
            navigationHeaderView.findViewById<AppCompatTextView>(R.id.nav_header_username)
        val navHeaderPlatform =
            navigationHeaderView.findViewById<AppCompatTextView>(R.id.nav_header_platform)

        if (newUser == User.LOGGED_OUT)
            navController.navigate(R.id.accounts_fragment)

        navHeaderUsername.text = newUser.name
        Glide.with(this)
            .load(newUser.getProfilePictureURL())
            .circleCrop()
            .placeholder(R.drawable.ic_account_circle_wallpafy_36dp)
            .fallback(R.drawable.ic_highlight_off_wallpafy_36dp)
            .into(navHeaderLogo)

        Glide.with(this)
            .load(newUser.getProfilePictureURL())
            .circleCrop()
            .placeholder(R.drawable.ic_account_circle_dark_wallpafy_36dp)
            .fallback(R.drawable.ic_account_circle_dark_wallpafy_36dp)
            .into(profilePicture)

        platformLogo.setImageResource(platformManager.currentPlatform.logoID)
        navHeaderPlatform.text = getString(platformManager.currentPlatform.displayNameID)

        if (this::accountsFragment.isInitialized)
            accountsFragment.onAccountChanged()

    }

}