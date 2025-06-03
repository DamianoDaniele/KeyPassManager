package com.personal.keypassmanager.data.local.mapper

import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.utils.EncryptionUtils

// Funzioni di mapping tra entità Room e modello dominio, con cifratura/decifratura.
fun Credential.toDomain(): CredentialDomain {
    return CredentialDomain(
        id = id,
        company = company,
        username = username,
        password = EncryptionUtils.decrypt(password)
    )
}

fun CredentialDomain.toEntity(): Credential {
    return Credential(
        id = id,
        company = company,
        username = username,
        password = EncryptionUtils.encrypt(password)
    )
}
