package com.tomsksmarttech.smart_alarm_mobile.spotify

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.getSpotifyPkceAuthorizationUrl
import com.adamratzman.spotify.getSpotifyPkceCodeChallenge
import com.tomsksmarttech.smart_alarm_mobile.R
import androidx.core.net.toUri
import com.adamratzman.spotify.spotifyClientPkceApi
import com.tomsksmarttech.smart_alarm_mobile.SharedData
import com.tomsksmarttech.smart_alarm_mobile.mqtt.MqttService


class SpotifyPkceLogin {
    suspend fun getAccessToken(activity: Activity) {
        val codeVerifier: String = (0..96).joinToString("") {
            (('a'..'z') + ('A'..'Z') + ('0'..'9')).random().toString()
        }
        SharedData.codeVerifier = codeVerifier
        val codeChallenge = getSpotifyPkceCodeChallenge(codeVerifier)
        val url: String = getSpotifyPkceAuthorizationUrl(
            SpotifyScope.UserReadCurrentlyPlaying,
            SpotifyScope.UserReadPlaybackState,
            SpotifyScope.UserModifyPlaybackState,
            SpotifyScope.UserReadPlaybackPosition,
            SpotifyScope.UserReadRecentlyPlayed,
            SpotifyScope.UserTopRead,
            SpotifyScope.UserLibraryRead,
            SpotifyScope.UserLibraryModify,
            SpotifyScope.UserFollowModify,
            SpotifyScope.UserFollowRead,
            SpotifyScope.UserReadPrivate,
            SpotifyScope.UserReadEmail,
            SpotifyScope.PlaylistReadCollaborative,
            SpotifyScope.PlaylistReadPrivate,
            SpotifyScope.PlaylistModifyPublic,
            SpotifyScope.PlaylistModifyPrivate,
            SpotifyScope.Streaming,
            SpotifyScope.UserReadCurrentlyPlaying,
            clientId = activity.getString(R.string.client_id),
            redirectUri = activity.getString(R.string.redirect_uri),
            codeChallenge = codeChallenge
        )
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        activity.startActivity(intent)
    }

    fun getAuthorizationCode(context: Context, intent: Intent) {
        intent.data.let { uri ->
            val code = uri?.getQueryParameter("code")
            if (code != null) {
                Log.d("SpotifyAuth", "Authorization Code: $code")
            } else {
                Log.e("SpotifyAuth", "Authorization failed or user denied access")
                Toast.makeText(context, "Authorization failed", Toast.LENGTH_SHORT).show()
            }
            SharedData.currentAuthorizationCode = code
        }
    }

    fun sendAuthorizationCode() {
        MqttService.addMsg("SpotifyAuth", SharedData.currentAuthorizationCode.toString())
    }

    suspend fun getCurrentTrackInfo(context: Context) {
        val code = SharedData.currentAuthorizationCode
        if (code == null) {
            Log.e("SpotifyAuth", "Authorization code is null")
            return
        }
        val api = spotifyClientPkceApi(
            context.getString(R.string.client_id),
            context.getString(R.string.redirect_uri),
            code,
            SharedData.codeVerifier
        ) {
            retryWhenRateLimited = false
        }.build()
        Log.d("Currently playing", api.library.getSavedTracks().items.toString())
    }
}