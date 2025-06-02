@file:OptIn(ExperimentalMaterial3Api::class)
package com.personal.keypassmanager.presentation.screen.credentials

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.personal.keypassmanager.data.model.CredentialDomain

@Composable
fun CredentialListScreen(
    credentials: List<CredentialDomain>,
    onAddCredential: () -> Unit,
    onEditCredential: (CredentialDomain) -> Unit
) {
    val (selected, setSelected) = remember { mutableStateOf<CredentialDomain?>(null) }
    val clipboardManager = LocalClipboardManager.current
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Le mie credenziali") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCredential) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi")
            }
        }
    ) { paddingValues ->
        if (selected == null) {
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(credentials) { credential ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clickable { setSelected(credential) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = credential.company, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = selected.company, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Username: ${selected.username}")
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(selected.username)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia username")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Password: ${selected.password}")
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(selected.password)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copia password")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { setSelected(null) }) {
                        Text("Indietro")
                    }
                }
            }
        }
    }
}
