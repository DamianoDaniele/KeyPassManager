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
import java.io.File
import java.io.FileWriter
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

    // Serializzazione/Deserializzazione JSON semplice (puoi sostituire con kotlinx.serialization se già presente)
    private fun buildJsonBackup(credentials: List<CredentialDomain>): String {
        return credentials.joinToString(",", prefix = "[", postfix = "]") {
            """{\"company\":\"${it.company}\",\"username\":\"${it.username}\",\"password\":\"${it.password}\"}"""
        }
    }
    private fun parseJsonBackup(json: String): List<CredentialDomain> {
        val regex = Regex("""\{"company":"(.*?)","username":"(.*?)","password":"(.*?)"}""")
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

    // Backup su Google Drive (appDataFolder)
    suspend fun backupToGoogleDrive(account: GoogleSignInAccount, credentials: List<CredentialDomain>, context: Context): Boolean {
        val token = DriveServiceHelper.getAccessToken(account, context) ?: return false
        val client = OkHttpClient()
        val jsonArray = JSONArray()
        credentials.forEach {
            val obj = JSONObject()
            obj.put("company", it.company)
            obj.put("username", it.username)
            obj.put("password", it.password)
            jsonArray.put(obj)
        }
        val json = jsonArray.toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toRequestBody(mediaType)
        val metadata = """{"name":"backup.json","parents":["appDataFolder"]}"""
        val multipartBody = okhttp3.MultipartBody.Builder().setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("metadata", null, metadata.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addFormDataPart("file", "backup.json", body)
            .build()
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(multipartBody)
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    // Ripristino da Google Drive (appDataFolder)
    suspend fun restoreFromGoogleDrive(account: GoogleSignInAccount, context: Context): List<CredentialDomain> {
        val token = DriveServiceHelper.getAccessToken(account, context) ?: return emptyList()
        val client = OkHttpClient()
        val listReq = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?q=name='backup.json'+and+trashed=false+and+'appDataFolder'+in+parents&spaces=appDataFolder&fields=files(id,name)")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        return try {
            val listResp = client.newCall(listReq).execute()
            val listBody = listResp.body?.string() ?: return emptyList()
            val files = JSONObject(listBody).optJSONArray("files") ?: return emptyList()
            if (files.length() == 0) return emptyList()
            val fileId = files.getJSONObject(0).getString("id")
            val getReq = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            val getResp = client.newCall(getReq).execute()
            val json = getResp.body?.string() ?: return emptyList()
            val result = mutableListOf<CredentialDomain>()
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(CredentialDomain(0, obj.getString("company"), obj.getString("username"), obj.getString("password")))
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
