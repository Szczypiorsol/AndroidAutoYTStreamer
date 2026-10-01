package com.example.androidautoytstreamer

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

class GoogleAuthManager(private val activity: Activity) {
    private val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions)

    fun signInIntent(): Intent = signInClient.signInIntent

    fun getCurrentAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(activity)

    fun signOut(onComplete: (() -> Unit)? = null) {
        signInClient.signOut().addOnCompleteListener(activity) {
            onComplete?.invoke()
        }
    }
}
