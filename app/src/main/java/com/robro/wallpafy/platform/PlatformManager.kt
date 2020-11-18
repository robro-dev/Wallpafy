package com.robro.wallpafy.platform

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.robro.wallpafy.MainActivity
import com.robro.wallpafy.R
import com.robro.wallpafy.platform.spotify.data.Album
import com.robro.wallpafy.platform.spotify.data.Playlist
import com.robro.wallpafy.platform.spotify.data.Playlist.Companion.RECENTLY_PLAYED_ID
import com.robro.wallpafy.platform.spotify.data.User
import net.openid.appauth.AuthState
import net.openid.appauth.AuthState.AuthStateAction
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService

/**
 * Manages all requests and the connection to music platforms
 *
 * @property currentPlatform platform to which the user is connected
 * @property currentUser information about the current user
 *
 * @param showErrors if true, toasts will be displayed in case of errors
 */
class PlatformManager(private val context: Context, private val showErrors: Boolean) {
    companion object {
        private const val TAG = "PlatformManager"

        private const val NULL = ""

        private const val PREFERENCE_NAME = "platform"
        private const val AUTH_STATE_KEY = "auth_state_json"
        private const val PLATFORM_STATE_KEY = "platform_state_json"

        private const val CURRENT_PLATFORM_PROPERTY = "current_platform"
        private const val CURRENT_USER = "current_user"
    }

    private var currentPlatformAPI = LogOutAPI(context) as PlatformAPI
    private var authState = AuthState()
    private val authService = AuthorizationService(context)

    private val connectionFailedToast =
        Toast.makeText(context, R.string.connection_failed_toast, Toast.LENGTH_LONG)

    var currentPlatform = PlatformEnum.LOG_OUT
        private set(value) {
            currentPlatformAPI = value.getPlatformAPI(context)
            authState = AuthState()
            field = value
        }

    var currentUser = User.LOGGED_OUT
        private set

    /**
     * Starts the AppAuth login activity for the current platform.
     *
     * @param platform platform to which the user is trying to log in
     * @param parentActivity activity that will receive the result of the login
     */
    fun startLoginActivity(
        platform: PlatformEnum,
        parentActivity: AppCompatActivity
    ) {
        currentPlatform = platform
        currentPlatformAPI.startLoginActivity(parentActivity)
        authState = AuthState(currentPlatformAPI.serviceConfig)
    }

    /**
     * Exchanges the authorization code got with [startLoginActivity] against a token to perform API calls to the platform
     *
     * @param response response send by the call of [startLoginActivity]
     * @param exception exception send by the call of [startLoginActivity]
     * @param successCallback method called if the token is well received. It take the new connected user as parameter
     * @param errorCallback method called if any error occurs during the process and the token is not received
     */
    fun requestToken(
        response: AuthorizationResponse?,
        exception: AuthorizationException?,
        successCallback: (User) -> Unit,
        errorCallback: () -> Unit
    ) {
        authState.update(response, exception)
        if (exception != null) {
            Log.e(TAG, exception.error + " : " + exception.errorDescription)
            currentPlatform = PlatformEnum.LOG_OUT
            connectionFailedToast.show()
            return
        }

        if (response != null) {
            authService.performTokenRequest(response.createTokenExchangeRequest()) { resp, ex ->
                authState.update(resp, ex)
                if (ex != null) {
                    Log.e(TAG, ex.error + " : " + ex.errorDescription)
                    currentPlatform = PlatformEnum.LOG_OUT
                    connectionFailedToast.show()
                    return@performTokenRequest
                }

                if (resp != null && authState.isAuthorized) {
                    updateUserInformation(successCallback, errorCallback)
                }
            }
        }
    }

    /**
     * Logs out the current user. Reset the state of the manager
     *
     * @param parentActivity activity to notify the change of the connection state
     */
    fun logOut(parentActivity: MainActivity) {
        currentPlatform = PlatformEnum.LOG_OUT
        currentUser = User.LOGGED_OUT
        authState = AuthState()

        saveState()
        parentActivity.onUserChanged(currentUser)
    }

    /**
     * Gets the current user's list of playlist
     *
     * @param successCallback method called when the list of playlist is well received
     * @param errorCallback method called when an error occurs during the process
     */
    fun getUserPlaylists(successCallback: (List<Playlist>) -> Unit, errorCallback: () -> Unit) {
        authState.performActionWithFreshTokens(authService,
            AuthStateAction { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, ex.error + " : " + ex.errorDescription)
                    connectionFailedToast.show()
                    return@AuthStateAction
                }

                if (accessToken != null) currentPlatformAPI.getUserPaylists(
                    accessToken,
                    successCallback,
                    errorCallback
                )

            })
    }

    /**
     * Gets the album list of a playlist
     *
     * @param playlistID ID of the playlist to get the album list. Use [Playlist.RECENTLY_PLAYED_ID] to get the list of recently played albums
     *
     * @param successCallback method called when the list of album is well received
     * @param errorCallback method called when an error occurs during the process
     */
    fun getAlbumList(
        playlistID: String,
        successCallback: (List<Album>) -> Unit,
        errorCallback: () -> Unit
    ) {
        authState.performActionWithFreshTokens(authService,
            AuthStateAction { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, ex.error + " : " + ex.errorDescription)
                    if (showErrors)
                        connectionFailedToast.show()
                    return@AuthStateAction
                }

                val ignoreCompilation = PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(context.getString(R.string.compilations_key), false)

                if (accessToken != null) {
                    if (playlistID == RECENTLY_PLAYED_ID) {
                        currentPlatformAPI.getRecentlyPlayed(
                            accessToken,
                            ignoreCompilation,
                            successCallback,
                            errorCallback
                        )
                    } else {
                        currentPlatformAPI.getPlaylistTracks(
                            accessToken,
                            ignoreCompilation,
                            playlistID,
                            successCallback,
                            errorCallback
                        )
                    }
                }
            })
    }

    /**
     * Load the state of the [PlatformManager] from the shared preferences
     *
     * @param successCallback method called when the state is well retrieved. Takes the loaded [User] in parameter
     * @param errorCallback method called when an error occurs during the process
     * @return true is the state is loaded, false otherwise
     */
    fun loadState(successCallback: (User) -> Unit = {}, errorCallback: () -> Unit = {}): Boolean {
        val platformPrefs = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
        val authStateJson = platformPrefs.getString(AUTH_STATE_KEY, NULL) ?: NULL
        val platformStateJson = platformPrefs.getString(PLATFORM_STATE_KEY, NULL) ?: NULL

        if (platformStateJson != NULL && authStateJson != NULL) {
            jsonDeserialize(platformStateJson)
            authState = AuthState.jsonDeserialize(authStateJson)
        }

        return if (isReady()) {
            updateUserInformation(successCallback, errorCallback)
            true
        } else {
            errorCallback()
            false
        }
    }

    /**
     * Saves the state of the [PlatformManager] to the shared preferences
     */
    fun saveState() {
        if (currentPlatform != PlatformEnum.LOG_OUT) {
            context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE).edit()
                .putString(PLATFORM_STATE_KEY, jsonSerialize())
                .putString(AUTH_STATE_KEY, authState.jsonSerializeString())
                .apply()
        } else {
            context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE).edit()
                .remove(PLATFORM_STATE_KEY)
                .remove(AUTH_STATE_KEY)
                .apply()
        }
    }

    /**
     * Determines whether the current authorization state is valid
     */
    fun isAuthorized(): Boolean = authState.isAuthorized

    /**
     * Determines whether the current state is valid to perform any requests to the platform
     */
    fun isReady(): Boolean {
        return currentPlatform != PlatformEnum.LOG_OUT && currentUser != User.LOGGED_OUT && isAuthorized()
    }

    /**
     * Updates the [currentUser]
     */
    private fun updateUserInformation(successCallback: (User) -> Unit, errorCallback: () -> Unit) {
        authState.performActionWithFreshTokens(authService,
            AuthStateAction { accessToken, _, ex ->
                if (ex != null) {
                    Log.e(TAG, ex.error + " : " + ex.errorDescription)
                    errorCallback()
                    if (showErrors) connectionFailedToast.show()
                    return@AuthStateAction
                }

                if (accessToken != null) {
                    currentPlatformAPI.getUserInformation(accessToken, {
                        currentUser = it
                        successCallback(it)
                    }, {
                        if (showErrors) connectionFailedToast.show()
                        errorCallback()
                    })
                }
            })

    }

    /**
     * Produces a JSON string representation of the current state for persistent storage
     *
     * @see jsonDeserialize
     */
    private fun jsonSerialize(): String {
        val json = JsonObject()

        json.apply {
            addProperty(CURRENT_PLATFORM_PROPERTY, currentPlatform.name)
            add(CURRENT_USER, Gson().toJsonTree(currentUser))
        }

        return json.toString()
    }

    /**
     * Reads a [PlatformManager] state from a JSON string representation produced by [jsonSerialize]
     */
    private fun jsonDeserialize(platformStateJson: String) {
        val json = JsonParser().parse(platformStateJson).asJsonObject

        currentPlatform =
            PlatformEnum.getPlatformEnumFromName(json.get(CURRENT_PLATFORM_PROPERTY).asString)
        currentUser = Gson().fromJson(json.get(CURRENT_USER), User::class.java)
    }
}