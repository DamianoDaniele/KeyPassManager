package com.personal.keypassmanager.drive

import android.content.Context
import android.os.Bundle
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object DriveServiceHelper {
    private const val TAG = "DriveServiceHelper"
    private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)?.also { account ->
            if (!GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
                return null
            }
        }
    }

    suspend fun getAccessToken(account: GoogleSignInAccount, context: Context): String? = withContext(Dispatchers.IO) {
        val realAccount = account.account ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(
                context,
                realAccount,
                "oauth2:$DRIVE_SCOPE",
                Bundle()
            ).also { token ->
                if (token.isNullOrEmpty()) {
                    throw IllegalStateException("Token is null or empty")
                }
            }
        } catch (e: UserRecoverableAuthException) {
            android.util.Log.w(TAG, "Recoverable auth error", e)
            null
        } catch (e: GoogleAuthException) {
            android.util.Log.e(TAG, "Google auth error", e)
            null
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Network error", e)
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Unexpected error", e)
            null
        }
    }
}
