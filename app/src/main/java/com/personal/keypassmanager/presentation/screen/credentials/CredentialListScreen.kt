@file:OptIn(ExperimentalMaterial3Api::class)
package com.personal.keypassmanager.presentation.screen.credentials

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.presentation.viewmodel.CredentialViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CredentialListScreen(
    credentials: List<CredentialDomain>,
    onAddCredential: () -> Unit,
    onEditCredential: (CredentialDomain) -> Unit,
    onDeleteCredential: (CredentialDomain) -> Unit,
    credentialViewModel: CredentialViewModel
) {
    val (selected, setSelected) = remember { mutableStateOf<CredentialDomain?>(null) }
    val (toDelete, setToDelete) = remember { mutableStateOf<CredentialDomain?>(null) }
    val showDeleteSheet = toDelete != null
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val (showExitDialog, setShowExitDialog) = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Le mie credenziali") })
        },
        floatingActionButton = {
            Column {
                FloatingActionButton(onClick = onAddCredential) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = {
                    scope.launch {
                        credentialViewModel.restoreAllBackups { restored ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (restored > 0) "$restored credenziali ripristinate dal backup" else "Nessuna nuova credenziale trovata nel backup"
                                )
                            }
                        }
                    }
                }) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Ripristina backup")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        AnimatedContent(targetState = selected, transitionSpec = { fadeIn() with fadeOut() }) { sel ->
            if (sel == null) {
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .combinedClickable(
                                    onClick = { setSelected(credential) },
                                    onLongClick = { setToDelete(credential) }
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sel.company,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = sel.username,
                                onValueChange = {},
                                label = { Text("Username") },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(sel.username))
                                        scope.launch { snackbarHostState.showSnackbar("Username copiato!") }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia username", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = sel.password,
                                onValueChange = {},
                                label = { Text("Password") },
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(sel.password))
                                        scope.launch { snackbarHostState.showSnackbar("Password copiata!") }
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia password", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { setSelected(null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Indietro")
                            }
                        }
                    }
                }
            }
        }
        if (showDeleteSheet && toDelete != null) {
            ModalBottomSheet(
                onDismissRequest = { setToDelete(null) },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Vuoi eliminare la credenziale di \"${toDelete.company}\"?", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = {
                            scope.launch {
                                sheetState.hide()
                                setToDelete(null)
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                            Text("Annulla")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = {
                            scope.launch {
                                onDeleteCredential(toDelete)
                                sheetState.hide()
                                setToDelete(null)
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Elimina")
                        }
                    }
                }
            }
        }
        // Intercetta il back hardware/software
        BackHandler(enabled = true) {
            setShowExitDialog(true)
        }
        if (showExitDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { setShowExitDialog(false) },
                title = { Text("Vuoi uscire da KeyPassManager?") },
                text = { Text("Sei sicuro di voler chiudere l'applicazione?") },
                confirmButton = {
                    Button(
                        onClick = { android.os.Process.killProcess(android.os.Process.myPid()) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Sì, esci") }
                },
                dismissButton = {
                    Button(
                        onClick = { setShowExitDialog(false) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) { Text("No, resta") }
                },
                shape = MaterialTheme.shapes.extraLarge
            )
        }
    }
}
