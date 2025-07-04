package com.personal.keypassmanager.presentation.viewmodel

import android.database.sqlite.SQLiteException
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.personal.keypassmanager.data.local.repository.CredentialRepository
import com.personal.keypassmanager.data.model.CredentialDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ViewModel che gestisce lo stato e la logica delle credenziali per la UI.
class CredentialViewModel(
    private val repository: CredentialRepository
) : ViewModel() {

    // Stato interno per la lista delle credenziali
    private val _credentials = MutableStateFlow<List<CredentialDomain>>(emptyList())
    val credentials: StateFlow<List<CredentialDomain>> = _credentials.asStateFlow()

    // Stato interno per la credenziale selezionata
    private val _selectedCredential = MutableStateFlow<CredentialDomain?>(null)
    val selectedCredential: StateFlow<CredentialDomain?> = _selectedCredential.asStateFlow()

    // Stato per errore database corrotto
    private val _dbCorruption = MutableStateFlow(false)
    val dbCorruption: StateFlow<Boolean> = _dbCorruption

    // Stato per errori generici da mostrare nella UI
    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        // Avvia l'osservazione delle credenziali dal repository
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllCredentials()
                    .collect { credentialList ->
                        _credentials.value = credentialList
                    }
            } catch (e: SQLiteException) {
                _dbCorruption.value = true
                _error.emit("Database error: ${e.message}")
            } catch (e: Exception) {
                _error.emit("Error loading credentials: ${e.message}")
            }
        }
    }

    // Seleziona una credenziale per la visualizzazione o modifica
    fun selectCredential(credential: CredentialDomain) {
        _selectedCredential.value = credential
    }

    // Deseleziona la credenziale attualmente selezionata
    fun clearSelectedCredential() {
        _selectedCredential.value = null
    }

    // Inserisce una nuova credenziale
    fun insertCredential(credential: CredentialDomain, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.insertCredential(credential)
                onComplete?.invoke()
            } catch (e: Exception) {
                _error.emit("Error inserting credential: ${e.message}")
            }
        }
    }

    // Ripristina tutte le credenziali dai backup
    fun restoreAllBackups(onResult: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val restored = repository.restoreAllBackups()
                onResult(restored)
            } catch (e: Exception) {
                _error.emit("Error restoring backups: ${e.message}")
                onResult(0)
            }
        }
    }

    // Aggiorna una credenziale esistente
    fun updateCredential(credential: CredentialDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateCredential(credential)
            } catch (e: Exception) {
                _error.emit("Error updating credential: ${e.message}")
            }
        }
    }

    // Elimina una credenziale
    fun deleteCredential(credential: CredentialDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteCredential(credential)
            } catch (e: Exception) {
                _error.emit("Error deleting credential: ${e.message}")
            }
        }
    }

    fun resetDatabaseAndRestore(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearDatabase()
                val restored = repository.restoreAllBackups()
                _dbCorruption.value = false
                onComplete(restored > 0)
            } catch (e: Exception) {
                _error.emit("Error resetting and restoring: ${e.message}")
                onComplete(false)
            }
        }
    }

    fun resetDatabase(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.clearDatabase()
                _dbCorruption.value = false
                onComplete()
            } catch (e: Exception) {
                _error.emit("Error resetting database: ${e.message}")
            }
        }
    }

    // Reset completo: elimina fisicamente il db e i file associati, poi ricrea il db vuoto
    fun hardResetDatabase(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.hardResetDatabase()
                _dbCorruption.value = false
                onComplete()
            } catch (e: Exception) {
                _error.emit("Error performing hard reset: ${e.message}")
            }
        }
    }

    // Reset e ripristino da backup: elimina fisicamente il db, ricrea il db e importa i backup
    fun hardResetAndRestoreFromBackup(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = repository.hardResetAndRestoreFromBackup()
                _dbCorruption.value = false
                onComplete(ok)
            } catch (e: Exception) {
                _error.emit("Error resetting and restoring from backup: ${e.message}")
                onComplete(false)
            }
        }
    }

    // Backup su Google Drive
    fun backupToGoogleDrive(account: GoogleSignInAccount, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = repository.backupToGoogleDrive(account, _credentials.value)
                onResult(ok)
            } catch (e: Exception) {
                _error.emit("Error backing up to Google Drive: ${e.message}")
                onResult(false)
            }
        }
    }

    // Ripristino da Google Drive
    fun restoreFromGoogleDrive(account: GoogleSignInAccount, onResult: (List<CredentialDomain>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val restored = repository.restoreFromGoogleDrive(account)
                // Inserisci tutte le credenziali ripristinate nel database
                restored.forEach { repository.insertCredential(it) }
                onResult(restored)
            } catch (e: Exception) {
                _error.emit("Error restoring from Google Drive: ${e.message}")
                onResult(emptyList())
            }
        }
    }
}
