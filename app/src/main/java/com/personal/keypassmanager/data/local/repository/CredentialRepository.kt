package com.personal.keypassmanager.data.local.repository

import android.content.Context
import com.personal.keypassmanager.data.local.dao.CredentialDao
import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.utils.EncryptionUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileWriter
import java.io.IOException

// Repository che gestisce l'accesso ai dati, backup e ripristino delle credenziali.
class CredentialRepository(private val credentialDao: CredentialDao, private val context: Context) {

    // Restituisce tutte le credenziali come flusso
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

    // Inserisce una nuova credenziale e la salva anche in backup
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

    /**
     * Ripristina tutte le credenziali dai file di backup nella memoria interna.
     * Salta i duplicati (stessa company, username e password).
     * Ritorna il numero di credenziali ripristinate.
     */
    suspend fun restoreAllBackups(): Int {
        val backupDir = File(context.filesDir, "keypassbackup")
        if (!backupDir.exists() || !backupDir.isDirectory) return 0
        val files = backupDir.listFiles() ?: return 0
        var restored = 0
        // Recupera tutte le credenziali già presenti
        val existing = credentialDao.getAllCredentials().first()
        for (file in files) {
            try {
                val lines = file.readLines()
                val company = lines.getOrNull(0)?.removePrefix("Company: ")?.trim() ?: continue
                val username = lines.getOrNull(1)?.removePrefix("Username: ")?.trim() ?: continue
                val password = lines.getOrNull(2)?.removePrefix("Password: ")?.trim() ?: continue
                // Controlla se già esiste
                val alreadyExists = existing.any { cred ->
                    cred.company == company &&
                    EncryptionUtils.decrypt(cred.username) == username &&
                    EncryptionUtils.decrypt(cred.password) == password
                }
                if (!alreadyExists) {
                    credentialDao.insertCredential(
                        Credential(
                            id = 0,
                            company = company,
                            username = EncryptionUtils.encrypt(username),
                            password = EncryptionUtils.encrypt(password)
                        )
                    )
                    restored++
                }
            } catch (_: Exception) {}
        }
        return restored
    }

    /**
     * Cancella tutte le credenziali dal database.
     */
    suspend fun clearDatabase() {
        val all = credentialDao.getAllCredentials().first()
        for (cred in all) {
            credentialDao.deleteCredential(cred)
        }
    }

    /**
     * Elimina fisicamente il database Room e i file associati (db, -shm, -wal).
     * Da chiamare in caso di corruzione per forzare la ricreazione del db.
     */
    private fun deleteDatabaseFiles() {
        val dbName = "keypass_encrypted.db"
        val dbFile = File(context.getDatabasePath(dbName).absolutePath)
        val shmFile = File(context.getDatabasePath(dbName).absolutePath + "-shm")
        val walFile = File(context.getDatabasePath(dbName).absolutePath + "-wal")
        dbFile.delete()
        shmFile.delete()
        walFile.delete()
    }

    /**
     * Reset completo: elimina fisicamente il db e i file associati, poi ricrea il db vuoto.
     */
    suspend fun hardResetDatabase() {
        deleteDatabaseFiles()
        // Forza la ricreazione del db accedendo al DAO
        credentialDao.getAllCredentials().first()
    }

    /**
     * Reset e ripristino da backup: elimina fisicamente il db, ricrea il db e importa i backup.
     * Ritorna true se almeno una credenziale è stata ripristinata.
     */
    suspend fun hardResetAndRestoreFromBackup(): Boolean {
        deleteDatabaseFiles()
        // Forza la ricreazione del db accedendo al DAO
        credentialDao.getAllCredentials().first()
        val restored = restoreAllBackups()
        return restored > 0
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
