package com.personal.keypassmanager.data.mapper

import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.domain.model.CredentialDomain
import com.personal.keypassmanager.utils.EncryptionUtils

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
