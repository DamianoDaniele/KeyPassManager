package com.personal.keypassmanager.data.model

// Modello dominio per la credenziale (usato nella logica di business/UI)
data class CredentialDomain(
    val id: Int = 0,
    val company: String,
    val username: String,
    val password: String
)
