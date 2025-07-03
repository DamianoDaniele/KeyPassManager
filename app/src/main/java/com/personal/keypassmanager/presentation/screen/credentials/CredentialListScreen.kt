@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
package com.personal.keypassmanager.presentation.screen.credentials

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.personal.keypassmanager.MainActivity
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.drive.DriveServiceHelper
import com.personal.keypassmanager.presentation.viewmodel.CredentialViewModel
import kotlinx.coroutines.launch

// Schermata principale per la gestione delle credenziali utente.
@Composable
fun CredentialListScreen(
    credentials: List<CredentialDomain>, // Lista delle credenziali da mostrare
    onAddCredential: () -> Unit, // Callback per aggiunta credenziale
    onEditCredential: (CredentialDomain) -> Unit, // Callback per modifica credenziale
    onDeleteCredential: (CredentialDomain) -> Unit, // Callback per eliminazione credenziale
    credentialViewModel: CredentialViewModel // ViewModel per la logica dati
) {
    // Stato per la credenziale selezionata (dettaglio)
    val (selected, setSelected) = remember { mutableStateOf<CredentialDomain?>(null) }
    // Stato per la credenziale da eliminare (bottom sheet)
    val (toDelete, setToDelete) = remember { mutableStateOf<CredentialDomain?>(null) }
    val showDeleteSheet = toDelete != null
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    // Stato per il dialog di uscita
    //val (showExitDialog, setShowExitDialog) = remember { mutableStateOf(false) }
    remember { mutableStateOf(false) }
    val (showDriveDialog, setShowDriveDialog) = remember { mutableStateOf(false) }
    val (isBackup, setIsBackup) = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val activity = context as? Activity

    // Scaffold principale con top bar, floating action button e snackbar
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Le mie credenziali") })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Pulsante per aggiungere una nuova credenziale
                FloatingActionButton(onClick = onAddCredential) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Pulsante per ripristino locale (come era prima)
                FloatingActionButton(onClick = {
                    scope.launch {
                        credentialViewModel.restoreAllBackups { restored ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (restored > 0) "$restored credenziali ripristinate dal backup locale" else "Nessuna nuova credenziale trovata nel backup locale"
                                )
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Ripristina backup locale")
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Pulsante Backup/Ripristino Google Drive
                FloatingActionButton(onClick = {
                    val account = DriveServiceHelper.getLastSignedInAccount(context)
                    if (account == null) {
                        // Avvia Google Sign-In senza callback diretto
                        if (activity is MainActivity) {
                            activity.launchGoogleSignIn()
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Impossibile avviare Google Sign-In")
                            }
                        }
                    } else {
                        setShowDriveDialog(true)
                    }
                }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup/Ripristino Drive")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Contenuto animato: lista credenziali o dettaglio
        AnimatedContent(targetState = selected, transitionSpec = { fadeIn() togetherWith fadeOut() }) { sel ->
            if (sel == null) {
                // Lista delle credenziali
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    items(credentials) { credential ->
                        // Card per ogni credenziale
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .combinedClickable(
                                    onClick = { setSelected(credential) }, // Mostra dettaglio
                                    onLongClick = { setToDelete(credential) } // Mostra bottom sheet elimina
                                ),
                            elevation = CardDefaults.cardElevation(4.dp),
                            border = if (selected?.company == credential.company) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = credential.company,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Dettaglio credenziale selezionata
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .wrapContentHeight()
                            .widthIn(max = 400.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Icona della ditta/servizio
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Nome della ditta/servizio
                            Text(
                                text = sel.company,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Username
                            Text(
                                text = "Username: ${sel.username}",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Pulsante per copiare l'username
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sel.username))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Username copiato negli appunti")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Copia Username")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Pulsante per copiare la password
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(sel.password))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Password copiata negli appunti")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Copia Password")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            // Pulsante per modificare la credenziale
                            Button(
                                onClick = { onEditCredential(sel) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Modifica Credenziale")
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet per conferma eliminazione
        if (toDelete != null) {
            ModalBottomSheet(
                onDismissRequest = { setToDelete(null) },
                sheetState = sheetState
            ) {
                // Contenuto del bottom sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Sei sicuro di voler eliminare questa credenziale?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Nome della ditta/servizio da eliminare
                        Text(
                            text = toDelete.company,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Pulsante per confermare eliminazione
                        Button(
                            onClick = {
                                onDeleteCredential(toDelete)
                                setToDelete(null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Elimina definitivamente")
                        }
                        // Pulsante per annullare
                        Button(
                            onClick = { setToDelete(null) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Annulla")
                        }
                    }
                }
            }
        }

        // Dialog per backup/ripristino Google Drive
        if (showDriveDialog) {
            AlertDialog(
                onDismissRequest = { setShowDriveDialog(false) },
                title = { Text("Backup/Ripristino Google Drive") },
                text = { Text("Vuoi eseguire un backup o ripristinare le credenziali da Google Drive?") },
                confirmButton = {
                    Button(onClick = {
                        val account = DriveServiceHelper.getLastSignedInAccount(context)
                        if (account != null) {
                            credentialViewModel.backupToGoogleDrive(account, context) { ok ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) "Backup su Drive completato" else "Backup su Drive fallito"
                                    )
                                }
                            }
                        }
                        setShowDriveDialog(false)
                    }) { Text("Backup") }
                },
                dismissButton = {
                    Button(onClick = {
                        val account = DriveServiceHelper.getLastSignedInAccount(context)
                        if (account != null) {
                            credentialViewModel.restoreFromGoogleDrive(account, context) { restored ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (restored.isNotEmpty()) "${restored.size} credenziali ripristinate da Drive" else "Nessun dato trovato su Drive"
                                    )
                                }
                            }
                        }
                        setShowDriveDialog(false)
                    }) { Text("Ripristina") }
                }
            )
        }

        // Gestione back press per chiudere il dettaglio o il bottom sheet
        BackHandler(enabled = selected != null || showDeleteSheet) {
            if (showDeleteSheet) {
                setToDelete(null)
            } else {
                setSelected(null)
            }
        }
    }
}

