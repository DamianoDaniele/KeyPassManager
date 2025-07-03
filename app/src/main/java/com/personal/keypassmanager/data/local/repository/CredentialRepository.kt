package com.personal.keypassmanager.data.local.repository

import android.content.Context
import com.personal.keypassmanager.data.local.dao.CredentialDao
import com.personal.keypassmanager.data.model.Credential
import com.personal.keypassmanager.data.model.CredentialDomain
import com.personal.keypassmanager.drive.DriveServiceHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.personal.keypassmanager.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.FileWriter
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

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
            val backupDir = java.io.File(context.filesDir, "keypassbackup")
            if (!backupDir.exists()) backupDir.mkdirs()
            val backupFile = java.io.File(backupDir, "backup_${System.currentTimeMillis()}.txt")
            java.io.FileWriter(backupFile, false).use { writer ->
                writer.write("Company: ${credential.company}\n")
                writer.write("Username: ${credential.username}\n")
                writer.write("Password: ${credential.password}\n")
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Ripristina tutte le credenziali dai file di backup nella memoria interna.
     * Salta i duplicati (stessa company, username e password).
     * Ritorna il numero di credenziali ripristinate.
     */
    suspend fun restoreAllBackups(): Int {
        val backupDir = java.io.File(context.filesDir, "keypassbackup")
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
        val dbFile = java.io.File(context.getDatabasePath(dbName).absolutePath)
        val shmFile = java.io.File(context.getDatabasePath(dbName).absolutePath + "-shm")
        val walFile = java.io.File(context.getDatabasePath(dbName).absolutePath + "-wal")
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

    // Serializzazione/Deserializzazione JSON semplice (puoi sostituire con kotlinx.serialization se già presente)
    private fun buildJsonBackup(credentials: List<CredentialDomain>): String {
        return credentials.joinToString(",", prefix = "[", postfix = "]") {
            "{\"company\":\"${it.company}\",\"username\":\"${it.username}\",\"password\":\"${it.password}\"}"
        }
    }
    private fun parseJsonBackup(json: String): List<CredentialDomain> {
        val regex = Regex("{\"company\":\"(.*?)\",\"username\":\"(.*?)\",\"password\":\"(.*?)\"}")
        return regex.findAll(json).map {
            val (company, username, password) = it.destructured
            CredentialDomain(0, company, username, password)
        }.toList()
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

    // Backup su Google Drive (appDataFolder) con cifratura AES-GCM e Retrofit
    suspend fun backupToGoogleDrive(account: GoogleSignInAccount, credentials: List<CredentialDomain>, context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = DriveServiceHelper.getAccessToken(account, context) ?: return@withContext false
            val api = provideGoogleDriveApi()
            val json = buildJsonBackup(credentials)
            val encryptedJson = com.personal.keypassmanager.utils.AESCipherHelper.encrypt(json)
            val metadataJson = """{\"name\":\"backup.json\",\"parents\":[\"appDataFolder\"]}"""
            val metadataBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                metadataJson
            )
            val fileBody = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/json; charset=utf-8"),
                encryptedJson
            )
            val metadataPart = okhttp3.MultipartBody.Part.createFormData("metadata", null, metadataBody)
            val filePart = okhttp3.MultipartBody.Part.createFormData("file", "backup.json", fileBody)
            val response = api.uploadFile(metadataPart, filePart, "Bearer $token")
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Ripristino da Google Drive (appDataFolder) con decifratura AES-GCM e Retrofit
    suspend fun restoreFromGoogleDrive(account: GoogleSignInAccount, context: Context): List<CredentialDomain> = withContext(Dispatchers.IO) {
        try {
            val token = DriveServiceHelper.getAccessToken(account, context) ?: return@withContext emptyList()
            val api = provideGoogleDriveApi()
            val listResp = api.listFiles(auth = "Bearer $token")
            if (!listResp.isSuccessful) return@withContext emptyList()
            val filesJson = listResp.body()?.string() ?: return@withContext emptyList()
            val filesArr = org.json.JSONObject(filesJson).optJSONArray("files") ?: return@withContext emptyList()
            if (filesArr.length() == 0) return@withContext emptyList()
            val fileId = filesArr.getJSONObject(0).getString("id")
            val getResp = api.downloadFile(fileId, "Bearer $token")
            if (!getResp.isSuccessful) return@withContext emptyList()
            val encrypted = getResp.body()?.string() ?: return@withContext emptyList()
            val json = com.personal.keypassmanager.utils.AESCipherHelper.decrypt(encrypted)
            parseJsonBackup(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Retrofit instance per Google Drive REST API
    private fun provideGoogleDriveApi(): com.personal.keypassmanager.drive.GoogleDriveApi {
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        return retrofit.create(com.personal.keypassmanager.drive.GoogleDriveApi::class.java)
    }
}
