package com.example.androidautoytstreamer

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import java.io.IOException

class GoogleAuthManager(private val activity: Activity) {
    private val youtubeScope = "https://www.googleapis.com/auth/youtube.readonly"

    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(youtubeScope))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions)

    fun signInIntent(): Intent = signInClient.signInIntent

    fun getCurrentAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)

    fun getAccessToken(): String? {
        val account = getCurrentAccount() ?: return null
        val accountName = account.account ?: return null

        return try {
            GoogleAuthUtil.getToken(activity, accountName, "oauth2:$youtubeScope")
        } catch (_: IOException) {
            null
        } catch (_: GoogleAuthException) {
            null
        }
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        signInClient.signOut().addOnCompleteListener(activity) {
            onComplete?.invoke()
        }
    }
}
