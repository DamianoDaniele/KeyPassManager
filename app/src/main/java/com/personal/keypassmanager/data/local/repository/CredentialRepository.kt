package com.personal.keypassmanager.data.repository

import com.personal.keypassmanager.data.local.dao.CredentialDao
import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.domain.model.CredentialDomain
import com.personal.keypassmanager.utils.EncryptionUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CredentialRepository(private val credentialDao: CredentialDao) {

    fun getAllCredentials(): Flow<List<CredentialDomain>> {
        return credentialDao.getAllCredentials().map { list ->
            list.map { credential ->
                credential.toDomain()
            }
        }
    }

    suspend fun getCredentialById(id: Int): CredentialDomain? {
        return credentialDao.getCredentialById(id)?.toDomain()
    }

    suspend fun insertCredential(credential: CredentialDomain) {
        credentialDao.insertCredential(credential.toEntity())
    }

    suspend fun updateCredential(credential: CredentialDomain) {
        credentialDao.updateCredential(credential.toEntity())
    }

    suspend fun deleteCredential(credential: CredentialDomain) {
        credentialDao.deleteCredential(credential.toEntity())
    }

    // Estensioni per la conversione tra Entity e Domain
    private fun Credential.toDomain(): CredentialDomain {
        return CredentialDomain(
            id = id,
            company = company,
            username = EncryptionUtils.decrypt(username),
            password = EncryptionUtils.decrypt(password)
        )
    }

    private fun CredentialDomain.toEntity(): Credential {
        return Credential(
            id = id,
            company = company,
            username = EncryptionUtils.encrypt(username),
            password = EncryptionUtils.encrypt(password)
        )
    }
}
