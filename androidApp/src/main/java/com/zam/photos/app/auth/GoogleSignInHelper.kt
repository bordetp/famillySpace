package com.zam.photos.app.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.GetSignInIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.zam.photos.app.BuildConfig
import com.zam.photos.app.debug.AuthDebugLog

sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

class GoogleSignInHelper {
    fun beginSignIn(
        activity: Activity,
        onReady: (IntentSenderRequest) -> Unit,
        onError: (GoogleSignInResult) -> Unit,
    ) {
        val (clientId, source) = resolveWebClientId(activity)
        if (clientId.isBlank()) {
            onError(GoogleSignInResult.Error("Google Sign-In non configuré (google.web.client.id)"))
            return
        }

        AuthDebugLog.log("Google: webClientId ($source) = $clientId")

        val request = GetSignInIntentRequest.builder()
            .setServerClientId(clientId)
            .build()

        AuthDebugLog.log("Google: demande Intent Sign-In")
        Identity.getSignInClient(activity)
            .getSignInIntent(request)
            .addOnSuccessListener { pendingIntent ->
                AuthDebugLog.log("Google: Intent prêt — ouverture sélecteur compte")
                onReady(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            }
            .addOnFailureListener { error ->
                AuthDebugLog.log("Google: échec Intent — ${error.message}")
                onError(mapFailure(error))
            }
    }

    fun parseSignInResult(activity: Activity, data: Intent?): GoogleSignInResult {
        if (data == null) {
            AuthDebugLog.log("Google: Intent data null → annulé")
            return GoogleSignInResult.Cancelled
        }

        return try {
            val credential = Identity.getSignInClient(activity).getSignInCredentialFromIntent(data)
            val idToken = credential.googleIdToken
            if (idToken.isNullOrBlank()) {
                AuthDebugLog.log("Google: credential sans idToken")
                GoogleSignInResult.Error("Jeton Google invalide")
            } else {
                AuthDebugLog.log("Google: idToken OK (${idToken.length} chars)")
                GoogleSignInResult.Success(idToken)
            }
        } catch (e: ApiException) {
            AuthDebugLog.log("Google: ApiException code=${e.statusCode} — ${e.message}")
            mapApiException(e)
        }
    }

    suspend fun signOut(context: Context) {
        val activity = context.findActivity() ?: return
        try {
            Identity.getSignInClient(activity).signOut()
        } catch (_: Exception) {
            // Best-effort: local session is still cleared by TokenStore
        }
    }

    private fun resolveWebClientId(context: Context): Pair<String, String> {
        val firebaseId = runCatching {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        }.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

        return if (firebaseId != null) {
            firebaseId to "firebase"
        } else {
            BuildConfig.GOOGLE_WEB_CLIENT_ID.trim() to "buildConfig"
        }
    }

    private fun mapFailure(error: Exception): GoogleSignInResult = when (error) {
        is ApiException -> mapApiException(error)
        else -> GoogleSignInResult.Error(formatError(error.message, null))
    }

    private fun mapApiException(error: ApiException): GoogleSignInResult = when (error.statusCode) {
        CommonStatusCodes.CANCELED, 12501 -> GoogleSignInResult.Cancelled
        10 -> GoogleSignInResult.Error(developerConfigMessage())
        else -> GoogleSignInResult.Error(formatError(error.message, error.statusCode))
    }

    private fun developerConfigMessage(): String =
        "Erreur Google (10) : SHA-1 Play App Signing non reconnu par Google Cloud.\n" +
            "Solution : Firebase → ajouter l'app Android + SHA-1 Play → télécharger google-services.json → rebuild."

    private fun formatError(message: String?, code: Int?): String {
        val codePart = code?.let { " (code $it)" }.orEmpty()
        return message?.takeIf { it.isNotBlank() }?.let { "$it$codePart" }
            ?: "Connexion Google impossible$codePart"
    }
}

fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
