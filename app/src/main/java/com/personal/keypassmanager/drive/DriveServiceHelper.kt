package com.personal.keypassmanager.drive

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DriveServiceHelper {
    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun getAccessToken(account: GoogleSignInAccount, context: Context): String? = withContext(Dispatchers.IO) {
        val realAccount = account.account
        if (realAccount == null) return@withContext null
        try {
            GoogleAuthUtil.getToken(context, realAccount, "oauth2:https://www.googleapis.com/auth/drive.appdata")
        } catch (e: Exception) {
            android.util.Log.e("DriveServiceHelper", "Errore accesso token Google Drive", e)
            null
        }
    }
}
