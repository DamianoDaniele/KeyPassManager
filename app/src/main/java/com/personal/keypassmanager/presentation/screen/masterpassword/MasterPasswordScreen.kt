package com.personal.keypassmanager.presentation.screen.masterpassword

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.personal.keypassmanager.data.local.DatabasePassphraseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

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
    var showSecurityDialog by remember { mutableStateOf(false) }
    var securityAnswer1 by remember { mutableStateOf("") }
    var securityAnswer2 by remember { mutableStateOf("") }
    var securityAnswer3 by remember { mutableStateOf("") }
    var securityInput1 by remember { mutableStateOf("") }
    var securityInput2 by remember { mutableStateOf("") }
    var securityInput3 by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var recoveredPassword by remember { mutableStateOf("") }
    var showRegistration by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(DatabasePassphraseProvider.isLoggedIn(context)) }
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
                label = { Text("Email di recupero (opzionale)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = securityAnswer1,
                onValueChange = { securityAnswer1 = it },
                label = { Text("Come si chiama tua madre?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null && securityAnswer1.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = securityAnswer2,
                onValueChange = { securityAnswer2 = it },
                label = { Text("La tua data di nascita? (gg/mm/aaaa)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null && securityAnswer2.isBlank()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = securityAnswer3,
                onValueChange = { securityAnswer3 = it },
                label = { Text("Dove vivi?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = error != null && securityAnswer3.isBlank()
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
        if (isLoggedIn) {
            // Mostra solo il logout se l'utente è loggato
            Button(onClick = {
                DatabasePassphraseProvider.logout(context)
                isLoggedIn = false
                masterPassword = ""
                error = null
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Logout")
            }
        } else {
            Button(
                onClick = {
                    if (!isSet) {
                        if (securityAnswer1.isBlank() || securityAnswer2.isBlank() || securityAnswer3.isBlank()) {
                            error = "Rispondi a tutte le domande di sicurezza."
                            return@Button
                        }
                        DatabasePassphraseProvider.saveMasterPasswordAndEmail(context, masterPassword, email)
                        DatabasePassphraseProvider.saveSecurityAnswers(context, securityAnswer1, securityAnswer2, securityAnswer3)
                        DatabasePassphraseProvider.setLoggedIn(context, true)
                        isLoggedIn = true
                        onUnlock()
                    } else {
                        if (DatabasePassphraseProvider.checkMasterPassword(context, masterPassword)) {
                            DatabasePassphraseProvider.setLoggedIn(context, true)
                            isLoggedIn = true
                            onUnlock()
                        } else {
                            error = "Codice errato. Riprova."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = masterPassword.isNotBlank() && (!isSet && securityAnswer1.isNotBlank() && securityAnswer2.isNotBlank() && securityAnswer3.isNotBlank() || isSet)
            ) {
                Text(if (!isSet) "Crea e accedi" else "Sblocca")
            }
            TextButton(onClick = { showSecurityDialog = true }) {
                Text("Password dimenticata?")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { showRegistration = true }) {
                    Text("Registrati")
                }
            }
            if (showSecurityDialog) {
                AlertDialog(
                    onDismissRequest = { showSecurityDialog = false },
                    title = { Text("Recupero password: domande di sicurezza") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = securityInput1,
                                onValueChange = { securityInput1 = it },
                                label = { Text("Come si chiama tua madre?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = securityInput2,
                                onValueChange = { securityInput2 = it },
                                label = { Text("La tua data di nascita? (gg/mm/aaaa)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = securityInput3,
                                onValueChange = { securityInput3 = it },
                                label = { Text("Dove vivi?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (error != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(error!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (DatabasePassphraseProvider.checkSecurityAnswers(context, securityInput1, securityInput2, securityInput3)) {
                                showPassword = true
                                // RECUPERO PASSWORD: usa solo la master password, non la passphrase del DB
                                recoveredPassword = DatabasePassphraseProvider.getMasterPassword(context)
                                error = null
                            } else {
                                error = "Risposte errate. Riprova."
                            }
                            showSecurityDialog = false
                        }) { Text("Conferma") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSecurityDialog = false }) { Text("Annulla") }
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
                            masterPassword = ""
                            error = null
                            showPassword = false
                            showSecurityDialog = false
                            // Qui puoi aggiungere logica per reimpostare la password
                        }) { Text("Reimposta") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPassword = false }) { Text("Chiudi") }
                    }
                )
            }
            if (showRegistration) {
                AlertDialog(
                    onDismissRequest = { showRegistration = false },
                    title = { Text("Registrazione utente") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email di recupero (opzionale)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = securityAnswer1,
                                onValueChange = { securityAnswer1 = it },
                                label = { Text("Come si chiama tua madre?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = securityAnswer2,
                                onValueChange = { securityAnswer2 = it },
                                label = { Text("La tua data di nascita? (gg/mm/aaaa)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = securityAnswer3,
                                onValueChange = { securityAnswer3 = it },
                                label = { Text("Dove vivi?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = masterPassword,
                                onValueChange = { masterPassword = it },
                                label = { Text("Crea una password principale") },
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
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (masterPassword.isNotBlank() && securityAnswer1.isNotBlank() && securityAnswer2.isNotBlank() && securityAnswer3.isNotBlank()) {
                                DatabasePassphraseProvider.saveMasterPasswordAndEmail(context, masterPassword, email)
                                DatabasePassphraseProvider.saveSecurityAnswers(context, securityAnswer1, securityAnswer2, securityAnswer3)
                                DatabasePassphraseProvider.setLoggedIn(context, true)
                                isLoggedIn = true
                                showRegistration = false
                                onUnlock()
                            } else {
                                error = "Compila tutti i campi."
                            }
                        }) { Text("Registra") }
                    }
                )
            }
        }
    }
}
