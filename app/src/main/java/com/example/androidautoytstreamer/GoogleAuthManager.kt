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

    fun getCurrentAccountIdentifier(): String? {
        val account = getCurrentAccount() ?: return null
        return account.account?.name ?: account.email
    }

    fun getAccessToken(): String? {
        val account = getCurrentAccount()
        if (account == null) {
            AppLog.d("GoogleAuthManager.getAccessToken: no signed-in Google account")
            return null
        }
        val accountName = account.account
        if (accountName == null) {
            AppLog.d("GoogleAuthManager.getAccessToken: account.account is null")
            return null
        }

        return try {
            val token = GoogleAuthUtil.getToken(activity, accountName, "oauth2:$youtubeScope")
            AppLog.d("GoogleAuthManager.getAccessToken: token successfully retrieved for ${accountName.name}")
            token
        } catch (e: IOException) {
            AppLog.e("GoogleAuthManager.getAccessToken IOException: ${e.message}", e)
            null
        } catch (e: GoogleAuthException) {
            AppLog.e("GoogleAuthManager.getAccessToken GoogleAuthException: ${e.message}", e)
            null
        }
    }

    fun signOut(onComplete: (() -> Unit)? = null) {
        AppLog.d("GoogleAuthManager.signOut initiated")
        signInClient.signOut().addOnCompleteListener(activity) {
            AppLog.d("GoogleAuthManager.signOut completed")
            onComplete?.invoke()
        }
    }
}
