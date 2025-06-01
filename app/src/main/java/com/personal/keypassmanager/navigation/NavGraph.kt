package com.personal.keypassmanager.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.personal.keypassmanager.presentation.screen.credentials.CredentialEditScreen
import com.personal.keypassmanager.presentation.screen.credentials.CredentialListScreen
import com.personal.keypassmanager.presentation.screen.masterpassword.MasterPasswordScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController, startDestination = "master_password") {
        composable("master_password") {
            MasterPasswordScreen(
                context = navController.context,
                onUnlock = {
                    navController.navigate("credential_list")
                }
            )
        }
        composable("credential_list") {
            // Passa le credenziali e le azioni necessarie
            CredentialListScreen(
                credentials = listOf(), // Sostituisci con i dati reali
                onAddCredential = {
                    navController.navigate("credential_edit")
                },
                onEditCredential = {
                    navController.navigate("credential_edit")
                }
            )
        }
        composable("credential_edit") {
            // Passa la credenziale da modificare se necessario
            CredentialEditScreen(
                credential = null,
                onSave = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}
