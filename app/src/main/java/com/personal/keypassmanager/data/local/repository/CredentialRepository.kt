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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

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
            val backupFile = java.io.File(backupDir, "backup_${System.currentTimeMillis()}.enc")
            val credentialJson = gson.toJson(credential)
            val encryptedData = com.personal.keypassmanager.utils.AESCipherHelper.encrypt(credentialJson)
            backupFile.writeText(encryptedData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Ripristina tutte le credenziali dai file di backup nella memoria interna.
     * Salta i duplicati (stessa company, username e password, normalizzati).
     * Ritorna il numero di credenziali ripristinate.
     */
    suspend fun restoreAllBackups(): Int {
        val backupDir = java.io.File(context.filesDir, "keypassbackup")
        if (!backupDir.exists() || !backupDir.isDirectory) return 0
        val files = backupDir.listFiles { _, name -> name.endsWith(".enc") } ?: return 0
        var restored = 0
        var failed = 0
        // Recupera tutte le credenziali già presenti
        val existing = credentialDao.getAllCredentials().first()
        for (file in files) {
            try {
                val encryptedData = file.readText()
                val credentialJson = com.personal.keypassmanager.utils.AESCipherHelper.decrypt(encryptedData)
                val credential = gson.fromJson(credentialJson, CredentialDomain::class.java)
                // Confronto normalizzato
                val alreadyExists = existing.any { cred ->
                    cred.company.trim().lowercase() == credential.company.trim().lowercase() &&
                    EncryptionUtils.decrypt(cred.username).trim().lowercase() == credential.username.trim().lowercase() &&
                    EncryptionUtils.decrypt(cred.password).trim() == credential.password.trim()
                }
                if (!alreadyExists) {
                    credentialDao.insertCredential(credential.toEntity())
                    restored++
                }
            } catch (e: Exception) {
                failed++
                e.printStackTrace()
            }
        }
        if (restored == 0 && failed > 0) {
            android.util.Log.w("CredentialRepository", "Nessuna credenziale ripristinata. File corrotti: $failed")
        }
        return restored
    }

    /**
     * Cancella tutte le credenziali dal database.
     */
    suspend fun clearDatabase() {
        credentialDao.deleteAll()
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

    private val gson = com.google.gson.Gson()

    // Serializzazione/Deserializzazione JSON semplice (puoi sostituire con kotlinx.serialization se già presente)
    private fun buildJsonBackup(credentials: List<CredentialDomain>): String {
        return gson.toJson(credentials)
    }
    private fun parseJsonBackup(json: String): List<CredentialDomain> {
        val type = object : com.google.gson.reflect.TypeToken<List<CredentialDomain>>() {}.type
        return gson.fromJson(json, type)
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
    suspend fun backupToGoogleDrive(account: GoogleSignInAccount, credentials: List<CredentialDomain>): Boolean = withContext(Dispatchers.IO) {
        val token = DriveServiceHelper.getAccessToken(account, context) ?: throw IOException("Failed to get access token")
        val api = provideGoogleDriveApi()
        val json = buildJsonBackup(credentials)
        val encryptedJson = com.personal.keypassmanager.utils.AESCipherHelper.encrypt(json)
        val metadataJson = """{"name":"backup.json","mimeType":"application/json"}"""
        val metadataBody = metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType())
        val filePart = okhttp3.MultipartBody.Part.createFormData(
            "file",
            "backup.json",
            encryptedJson.toRequestBody("application/octet-stream".toMediaType())
        )
        val response = api.uploadFile(metadataBody, filePart, "Bearer $token")
        if (!response.isSuccessful) {
            android.util.Log.e("DriveBackup", "Errore backup: " + response.code() + " - " + response.errorBody()?.string())
            throw IOException("Failed to upload backup: "+ response.errorBody()?.string())
        }
        response.isSuccessful
    }

    // Ripristino da Google Drive (appDataFolder) con decifratura AES-GCM e Retrofit
    suspend fun restoreFromGoogleDrive(account: GoogleSignInAccount): List<CredentialDomain> = withContext(Dispatchers.IO) {
        val token = DriveServiceHelper.getAccessToken(account, context) ?: throw IOException("Failed to get access token")
        val api = provideGoogleDriveApi()
        // Cerca nella root di Drive
        val query = "name = 'backup.json' and trashed = false"
        val listResp = api.listFiles(query = query, auth = "Bearer $token")
        if (!listResp.isSuccessful) {
            android.util.Log.e("DriveRestore", "Errore lista file: ${listResp.code()} - ${listResp.errorBody()?.string()}")
            throw IOException("Failed to list files: ${listResp.errorBody()?.string()}")
        }
        val filesJson = listResp.body()?.string() ?: return@withContext emptyList()
        val filesArr = org.json.JSONObject(filesJson).optJSONArray("files") ?: return@withContext emptyList()
        if (filesArr.length() == 0) {
            android.util.Log.e("DriveRestore", "Nessun file backup.json trovato nella root di Drive")
            return@withContext emptyList()
        }
        val fileId = filesArr.getJSONObject(0).getString("id")
        android.util.Log.d("DriveRestore", "Trovato file backup.json con id: $fileId")
        val getResp = api.downloadFile(fileId, "Bearer $token")
        if (!getResp.isSuccessful) {
            android.util.Log.e("DriveRestore", "Errore download: ${getResp.code()} - ${getResp.errorBody()?.string()}")
            throw IOException("Failed to download file: ${getResp.errorBody()?.string()}")
        }
        val encrypted = getResp.body()?.string() ?: return@withContext emptyList()
        android.util.Log.d("DriveRestore", "Contenuto file scaricato: $encrypted")
        val json = try {
            com.personal.keypassmanager.utils.AESCipherHelper.decrypt(encrypted)
        } catch (e: Exception) {
            android.util.Log.e("DriveRestore", "Errore decifratura: ${e.message}")
            return@withContext emptyList()
        }
        android.util.Log.d("DriveRestore", "Contenuto file decifrato: $json")
        val restored = try {
            parseJsonBackup(json)
        } catch (e: Exception) {
            android.util.Log.e("DriveRestore", "Errore parsing JSON: ${e.message}")
            return@withContext emptyList()
        }
        // Evita duplicati: confronta company, username e password normalizzati
        val existing = credentialDao.getAllCredentials().first()
        val toInsert = restored.filter { credential ->
            existing.none { cred ->
                cred.company.trim().lowercase() == credential.company.trim().lowercase() &&
                EncryptionUtils.decrypt(cred.username).trim().lowercase() == credential.username.trim().lowercase() &&
                EncryptionUtils.decrypt(cred.password).trim() == credential.password.trim()
            }
        }
        toInsert.forEach { credentialDao.insertCredential(it.toEntity()) }
        android.util.Log.d("DriveRestore", "Credenziali effettivamente inserite: ${toInsert.size}")
        toInsert
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
