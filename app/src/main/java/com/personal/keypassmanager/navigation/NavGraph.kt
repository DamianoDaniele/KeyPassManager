package com.personal.keypassmanager.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.personal.keypassmanager.data.local.database.AppDatabase
import com.personal.keypassmanager.data.local.repository.CredentialRepository
import com.personal.keypassmanager.presentation.screen.credentials.CredentialEditScreen
import com.personal.keypassmanager.presentation.screen.credentials.CredentialListScreen
import com.personal.keypassmanager.presentation.screen.masterpassword.MasterPasswordScreen
import com.personal.keypassmanager.presentation.viewmodel.CredentialViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// Gestisce la navigazione tra le schermate principali dell'app.
@Composable
fun NavGraph(navController: NavHostController) {
    // Inizializza il ViewModel e il repository
    val context = navController.context
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { CredentialRepository(db.credentialDao(), context) }
    val credentialViewModel: CredentialViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return CredentialViewModel(repository) as T
        }
    })
    val credentials by credentialViewModel.credentials.collectAsState()
    val dbCorruption by credentialViewModel.dbCorruption.collectAsState()

    NavHost(navController, startDestination = "master_password") {
        composable("master_password") {
            MasterPasswordScreen(
                context = context,
                onUnlock = {
                    navController.navigate("credential_list")
                },
                dbCorruption = dbCorruption,
                onResetAndRestore = { onComplete -> credentialViewModel.hardResetAndRestoreFromBackup(onComplete) },
                onReset = credentialViewModel::hardResetDatabase
            )
        }
        composable("credential_list") {
            CredentialListScreen(
                credentials = credentials,
                onAddCredential = {
                    navController.navigate("credential_edit")
                },
                onEditCredential = {
                    navController.navigate("credential_edit")
                },
                onDeleteCredential = { cred ->
                    credentialViewModel.deleteCredential(cred)
                },
                credentialViewModel = credentialViewModel
            )
        }
        composable("credential_edit") {
            CredentialEditScreen(
                credential = null,
                onSave = { cred ->
                    credentialViewModel.insertCredential(cred) {
                        navController.popBackStack()
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }
}
