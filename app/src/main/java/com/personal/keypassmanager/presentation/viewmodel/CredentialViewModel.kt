package com.personal.keypassmanager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.keypassmanager.data.local.repository.CredentialRepository
import com.personal.keypassmanager.data.model.CredentialDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CredentialViewModel(
    private val repository: CredentialRepository
) : ViewModel() {

    // Stato interno per la lista delle credenziali
    private val _credentials = MutableStateFlow<List<CredentialDomain>>(emptyList())
    val credentials: StateFlow<List<CredentialDomain>> = _credentials.asStateFlow()

    // Stato interno per la credenziale selezionata
    private val _selectedCredential = MutableStateFlow<CredentialDomain?>(null)
    val selectedCredential: StateFlow<CredentialDomain?> = _selectedCredential.asStateFlow()

    init {
        // Avvia l'osservazione delle credenziali dal repository
        viewModelScope.launch {
            repository.getAllCredentials()
                .collect { credentialList ->
                    _credentials.value = credentialList
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
    fun insertCredential(credential: CredentialDomain) {
        viewModelScope.launch {
            repository.insertCredential(credential)
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
}
