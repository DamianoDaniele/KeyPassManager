package com.personal.keypassmanager.data.local.repository

import android.content.Context
import com.personal.keypassmanager.data.local.dao.CredentialDao
import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.utils.EncryptionUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CredentialRepository(private val credentialDao: CredentialDao, private val context: Context) {

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
        backupCredentialToFile(credential)
    }

    suspend fun updateCredential(credential: CredentialDomain) {
        credentialDao.updateCredential(credential.toEntity())
    }

    suspend fun deleteCredential(credential: CredentialDomain) {
        credentialDao.deleteCredential(credential.toEntity())
    }

    private fun backupCredentialToFile(credential: CredentialDomain) {
        try {
            val backupDir = File(context.filesDir, "keypassbackup")
            if (!backupDir.exists()) backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.txt")
            FileWriter(backupFile, false).use { writer ->
                writer.write("Company: ${credential.company}\n")
                writer.write("Username: ${credential.username}\n")
                writer.write("Password: ${credential.password}\n")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
