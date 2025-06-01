package com.personal.keypassmanager.presentation.screen.masterpassword

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.personal.keypassmanager.data.local.DatabasePassphraseProvider
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MasterPasswordScreen(
    context: Context,
    onUnlock: () -> Unit
) {
    var masterPassword by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val isSet = remember { DatabasePassphraseProvider.isMasterPasswordSet(context) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmailSent by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var recoveredPassword by remember { mutableStateOf("") }
    var smtpUser by remember { mutableStateOf("") }
    var smtpPassword by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var sendSuccess by remember { mutableStateOf(false) }
    var smtpShowPassword by remember { mutableStateOf(false) }
    var showAppPasswordHelp by remember { mutableStateOf(false) }
    val coroutineScope = remember { CoroutineScope(Dispatchers.Main) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (!isSet) "Crea il codice principale" else "Inserisci il codice principale",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (!isSet) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email di recupero") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null && email.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = masterPassword,
            onValueChange = { masterPassword = it },
            label = { Text("Codice") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (isPasswordVisible)
                    Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(imageVector = image, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null && masterPassword.isBlank()
        )
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                if (!isSet) {
                    if (email.isBlank()) {
                        error = "Inserisci una email valida."
                        return@Button
                    }
                    DatabasePassphraseProvider.saveMasterPasswordAndEmail(context, masterPassword, email)
                    onUnlock()
                } else {
                    if (DatabasePassphraseProvider.checkMasterPassword(context, masterPassword)) {
                        onUnlock()
                    } else {
                        error = "Codice errato. Riprova."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = masterPassword.isNotBlank() && (isSet || email.isNotBlank())
        ) {
            Text(if (!isSet) "Crea e accedi" else "Sblocca")
        }
        TextButton(onClick = { showResetDialog = true }) {
            Text("Password dimenticata?")
        }
        if (showResetDialog) {
            val recoveryEmail = DatabasePassphraseProvider.getUserEmail(context)
            if (recoveryEmail.isNullOrBlank()) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Recupero password") },
                    text = { Text("Nessuna email salvata. Non puoi recuperare la password.") },
                    confirmButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("OK") }
                    }
                )
            } else if (!resetEmailSent) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Recupero password") },
                    text = {
                        Column {
                            Text("Inviare un codice di recupero a $recoveryEmail?")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smtpUser,
                                onValueChange = { smtpUser = it },
                                label = { Text("Email SMTP mittente (Gmail)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false // Campo disabilitato, precompilato
                            )
                            LaunchedEffect(Unit) { smtpUser = recoveryEmail }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = smtpPassword,
                                onValueChange = {
                                    if (it.length <= 16 && it.all { c -> c.isLetterOrDigit() }) smtpPassword = it
                                },
                                label = { Text("App Password Gmail") },
                                singleLine = true,
                                visualTransformation = if (smtpShowPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val image = if (smtpShowPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { smtpShowPassword = !smtpShowPassword }) {
                                        Icon(imageVector = image, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            TextButton(onClick = { showAppPasswordHelp = true }) {
                                Text("Come ottenere la App Password?")
                            }
                            if (sending) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                            if (sendError != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(sendError!!, color = MaterialTheme.colorScheme.error)
                            }
                            if (sendSuccess) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Email inviata con successo!", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val code = UUID.randomUUID().toString().take(6)
                            val expiry = System.currentTimeMillis() + 10 * 60 * 1000 // 10 min
                            DatabasePassphraseProvider.saveResetCode(context, code, expiry)
                            sending = true
                            sendError = null
                            sendSuccess = false
                            coroutineScope.launch {
                                try {
                                    val smtpHost = "smtp.gmail.com"
                                    val smtpPort = "587"
                                    DatabasePassphraseProvider.sendResetEmailSmtp(
                                        smtpHost = smtpHost,
                                        smtpPort = smtpPort,
                                        smtpUser = smtpUser,
                                        smtpPassword = smtpPassword,
                                        toEmail = recoveryEmail,
                                        code = code
                                    )
                                    sending = false
                                    sendSuccess = true
                                    resetEmailSent = true
                                    showCodeDialog = true
                                } catch (e: Exception) {
                                    sending = false
                                    sendError = "Errore invio: "+(e.localizedMessage ?: e.toString())
                                }
                            }
                        }, enabled = !sending && smtpPassword.length == 16) { Text("Invia") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) { Text("Annulla") }
                    }
                )
            }
        }
        if (showCodeDialog) {
            AlertDialog(
                onDismissRequest = { showCodeDialog = false },
                title = { Text("Inserisci codice ricevuto") },
                text = {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it },
                        label = { Text("Codice") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val (savedCode, expiry) = DatabasePassphraseProvider.getResetCode(context)
                        if (codeInput == savedCode && System.currentTimeMillis() < expiry) {
                            showPassword = true
                            recoveredPassword = DatabasePassphraseProvider.getOrCreateDatabasePassphrase(context)
                            DatabasePassphraseProvider.clearResetCode(context)
                        } else {
                            error = "Codice errato o scaduto."
                        }
                        showCodeDialog = false
                    }) { Text("Conferma") }
                },
                dismissButton = {
                    TextButton(onClick = { showCodeDialog = false }) { Text("Annulla") }
                }
            )
        }
        if (showPassword) {
            AlertDialog(
                onDismissRequest = { showPassword = false },
                title = { Text("Password recuperata") },
                text = { Text("La tua password è: $recoveredPassword\nVuoi reimpostarla?") },
                confirmButton = {
                    TextButton(onClick = {
                        // Reimposta password
                        masterPassword = ""
                        error = null
                        showPassword = false
                        showResetDialog = false
                        // Qui puoi aggiungere logica per reimpostare la password
                    }) { Text("Reimposta") }
                },
                dismissButton = {
                    TextButton(onClick = { showPassword = false }) { Text("Chiudi") }
                }
            )
        }
        if (showAppPasswordHelp) {
            AlertDialog(
                onDismissRequest = { showAppPasswordHelp = false },
                title = { Text("Come ottenere la App Password Gmail") },
                text = {
                    Text("1. Attiva la verifica in due passaggi su https://myaccount.google.com/security.\n" +
                            "2. Vai su https://myaccount.google.com/apppasswords.\n" +
                            "3. Genera una nuova App Password per 'Posta' o 'Altro'.\n" +
                            "4. Copia la password generata (16 caratteri) e incollala qui.")
                },
                confirmButton = {
                    TextButton(onClick = { showAppPasswordHelp = false }) { Text("OK") }
                }
            )
        }
    }
}
