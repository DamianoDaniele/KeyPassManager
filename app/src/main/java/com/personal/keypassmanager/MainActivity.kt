package com.personal.keypassmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.personal.keypassmanager.drive.DriveServiceHelper
import com.personal.keypassmanager.navigation.NavGraph
import com.personal.keypassmanager.ui.theme.KeyPassManagerTheme
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

// Activity principale che avvia la UI Compose e la navigazione.
class MainActivity : ComponentActivity() {

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            // Login Google riuscito, ora puoi usare DriveServiceHelper.getDriveService(...)
            Toast.makeText(this, "Accesso Google Drive riuscito", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Accesso Google Drive fallito: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeyPassManagerTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }

    // Metodo pubblico per avviare il login Google da Compose
    fun launchGoogleSignIn() {
        val client = DriveServiceHelper.getSignInClient(this)
        googleSignInLauncher.launch(client.signInIntent)
    }
}