package com.personal.keypassmanager.presentation.viewmodel

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.keypassmanager.data.local.repository.CredentialRepository
import com.personal.keypassmanager.data.model.CredentialDomain
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

    init {
        // Avvia l'osservazione delle credenziali dal repository
        viewModelScope.launch {
            try {
                repository.getAllCredentials()
                    .collect { credentialList ->
                        _credentials.value = credentialList
                    }
            } catch (e: SQLiteException) {
                _dbCorruption.value = true
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
        viewModelScope.launch {
            repository.insertCredential(credential)
            onComplete?.invoke()
        }
    }

    // Ripristina tutte le credenziali dai backup
    fun restoreAllBackups(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val restored = repository.restoreAllBackups()
            onResult(restored)
        }
    }

    // Aggiorna una credenziale esistente
    fun updateCredential(credential: CredentialDomain) {
        viewModelScope.launch {
            repository.updateCredential(credential)
        }
    }

    // Elimina una credenziale
    fun deleteCredential(credential: CredentialDomain) {
        viewModelScope.launch {
            repository.deleteCredential(credential)
        }
    }

    fun resetDatabaseAndRestore(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearDatabase()
                val restored = repository.restoreAllBackups()
                _dbCorruption.value = false
                onComplete(restored > 0)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    fun resetDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearDatabase()
            _dbCorruption.value = false
            onComplete()
        }
    }

    // Reset completo: elimina fisicamente il db e i file associati, poi ricrea il db vuoto
    fun hardResetDatabase(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.hardResetDatabase()
            _dbCorruption.value = false
            onComplete()
        }
    }

    // Reset e ripristino da backup: elimina fisicamente il db, ricrea il db e importa i backup
    fun hardResetAndRestoreFromBackup(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.hardResetAndRestoreFromBackup()
            _dbCorruption.value = false
            onComplete(ok)
        }
    }
}
