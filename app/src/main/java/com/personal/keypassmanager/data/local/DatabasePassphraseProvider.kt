package com.personal.keypassmanager.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.personal.keypassmanager.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// Provider per la gestione sicura della passphrase e chiavi di cifratura del database.
object DatabasePassphraseProvider {

    private const val PREFS_NAME = "encrypted_prefs"
    private const val PASSPHRASE_KEY = "db_passphrase" // This is now ONLY the master password
    private const val DB_ENCRYPTION_KEY = "db_encryption_key" // New key for database encryption
    private const val SECURITY_ANSWER1_KEY = "security_answer1"
    private const val SECURITY_ANSWER2_KEY = "security_answer2"
    private const val SECURITY_ANSWER3_KEY = "security_answer3"
    private const val LOGGED_IN_KEY = "is_logged_in"
    private const val LAST_ACTIVE_KEY = "last_active"
    private const val SESSION_TIMEOUT_MINUTES = 15L // Timeout di 15 minuti

    // Restituisce la SupportFactory per Room con la chiave derivata
    fun getSupportFactory(password: String): SupportFactory {
        val passphrase = deriveKeyFromPassword(password)
        return SupportFactory(passphrase)
    }

    // Deriva una chiave sicura dalla password
    private fun deriveKeyFromPassword(password: String): ByteArray {
        val salt = "fixed_salt_placeholder".toByteArray() // Should be unique per user in production
        val iterations = 10000
        val keyLength = 256 // 256 bits for AES-256

        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    // Versione sospesa per accesso sicuro alle EncryptedSharedPreferences
    private suspend fun getEncryptedSharedPreferencesAsync(context: Context): SharedPreferences = withContext(Dispatchers.IO) {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        try {
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            val isBadTag = e is javax.crypto.AEADBadTagException
            val isProtoBuf = e.javaClass.name == "com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException"
            if (isBadTag || isProtoBuf) {
                context.deleteSharedPreferences(PREFS_NAME)
                EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                throw e
            }
        }
    }

    // Versione sincrona per retrocompatibilità interna (solo per uso interno, evitare in UI)
    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return try {
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            val isBadTag = e is javax.crypto.AEADBadTagException
            val isProtoBuf = e.javaClass.name == "com.google.crypto.tink.shaded.protobuf.InvalidProtocolBufferException"
            if (isBadTag || isProtoBuf) {
                context.deleteSharedPreferences(PREFS_NAME)
                EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } else {
                throw e
            }
        }
    }

    // Restituisce la passphrase del database, generandola se non esiste (sospesa)
    suspend fun getOrCreateDatabasePassphrase(context: Context): String = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        var passphrase = prefs.getString(DB_ENCRYPTION_KEY, null)
        if (passphrase == null) {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(DB_ENCRYPTION_KEY, passphrase).apply()
        }
        passphrase ?: ""
    }

    // Versione sincrona per uso Room/legacy
    fun getOrCreateDatabasePassphraseSync(context: Context): String {
        val prefs = getEncryptedSharedPreferences(context)
        var passphrase = prefs.getString(DB_ENCRYPTION_KEY, null)
        if (passphrase == null) {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(DB_ENCRYPTION_KEY, passphrase).apply()
        }
        return passphrase ?: ""
    }

    // Controlla se la password master è impostata (sospesa)
    suspend fun isMasterPasswordSet(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.getString(PASSPHRASE_KEY, null) != null
    }

    // Salva la password master (sospesa)
    suspend fun saveMasterPassword(context: Context, password: String) = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.edit().putString(PASSPHRASE_KEY, password).apply()
    }

    // Controlla se la password master fornita corrisponde a quella salvata (sospesa)
    suspend fun checkMasterPassword(context: Context, password: String): Boolean = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        val saved = prefs.getString(PASSPHRASE_KEY, null)
        saved == password
    }

    // Restituisce la password master salvata (sospesa)
    suspend fun getMasterPassword(context: Context): String = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.getString(PASSPHRASE_KEY, "") ?: ""
    }

    // Salva le risposte alle domande di sicurezza (sospesa)
    suspend fun saveSecurityAnswers(context: Context, answer1: String, answer2: String, answer3: String) = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.edit()
            .putString(SECURITY_ANSWER1_KEY, EncryptionUtils.encrypt(answer1.trim().lowercase()))
            .putString(SECURITY_ANSWER2_KEY, EncryptionUtils.encrypt(answer2.trim().lowercase()))
            .putString(SECURITY_ANSWER3_KEY, EncryptionUtils.encrypt(answer3.trim().lowercase()))
            .apply()
    }

    // Controlla se le risposte alle domande di sicurezza corrispondono a quelle salvate (sospesa)
    suspend fun checkSecurityAnswers(context: Context, answer1: String, answer2: String, answer3: String): Boolean = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        val a1 = prefs.getString(SECURITY_ANSWER1_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        val a2 = prefs.getString(SECURITY_ANSWER2_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        val a3 = prefs.getString(SECURITY_ANSWER3_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        a1 == answer1.trim().lowercase() &&
        a2 == answer2.trim().lowercase() &&
        a3 == answer3.trim().lowercase()
    }

    // Controlla se sono state impostate tutte le risposte alle domande di sicurezza (sospesa)
    suspend fun areSecurityAnswersSet(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.getString(SECURITY_ANSWER1_KEY, null) != null &&
        prefs.getString(SECURITY_ANSWER2_KEY, null) != null &&
        prefs.getString(SECURITY_ANSWER3_KEY, null) != null
    }

    // Imposta lo stato di accesso dell'utente (sospesa)
    suspend fun setLoggedIn(context: Context, loggedIn: Boolean) = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.edit().putBoolean(LOGGED_IN_KEY, loggedIn)
            .putLong(LAST_ACTIVE_KEY, System.currentTimeMillis())
            .apply()
    }

    // Controlla se l'utente è attualmente connesso (sospesa)
    suspend fun isLoggedIn(context: Context): Boolean = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        val loggedIn = prefs.getBoolean(LOGGED_IN_KEY, false)
        val lastActive = prefs.getLong(LAST_ACTIVE_KEY, 0L)
        val now = System.currentTimeMillis()
        val timeoutMillis = SESSION_TIMEOUT_MINUTES * 60 * 1000
        if (loggedIn && now - lastActive < timeoutMillis) {
            // Aggiorna il timestamp di attività
            prefs.edit().putLong(LAST_ACTIVE_KEY, now).apply()
            true
        } else {
            // Timeout scaduto o non loggato
            logoutAsync(context)
            false
        }
    }

    // Disconnette l'utente e cancella i dati di accesso (sospesa)
    suspend fun logoutAsync(context: Context) = withContext(Dispatchers.IO) {
        val prefs = getEncryptedSharedPreferencesAsync(context)
        prefs.edit().clear().apply()
    }

    // Versione sincrona per compatibilità legacy (da evitare in UI)
    fun logout(context: Context) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().clear().apply()
    }
}
