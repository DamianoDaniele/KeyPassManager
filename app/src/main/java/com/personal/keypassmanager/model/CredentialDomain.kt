package com.personal.keypassmanager.domain.model

data class CredentialDomain(
    val id: Int = 0,
    val company: String,
    val username: String,
    val password: String
)
