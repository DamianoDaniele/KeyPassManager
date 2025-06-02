package com.personal.keypassmanager.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.personal.keypassmanager.utils.EncryptionUtils
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object DatabasePassphraseProvider {

    private const val PREFS_NAME = "encrypted_prefs"
    private const val PASSPHRASE_KEY = "db_passphrase" // This is now ONLY the master password
    private const val DB_ENCRYPTION_KEY = "db_encryption_key" // New key for database encryption
    private const val SECURITY_ANSWER1_KEY = "security_answer1"
    private const val SECURITY_ANSWER2_KEY = "security_answer2"
    private const val SECURITY_ANSWER3_KEY = "security_answer3"
    private const val EMAIL_KEY = "user_email"
    private const val LOGGED_IN_KEY = "is_logged_in"
    private const val LAST_ACTIVE_KEY = "last_active"
    private const val SESSION_TIMEOUT_MINUTES = 15L // Timeout di 15 minuti

    fun getSupportFactory(password: String): SupportFactory {
        val passphrase = deriveKeyFromPassword(password)
        return SupportFactory(passphrase)
    }

    private fun deriveKeyFromPassword(password: String): ByteArray {
        val salt = "fixed_salt_placeholder".toByteArray() // Should be unique per user in production
        val iterations = 10000
        val keyLength = 256 // 256 bits for AES-256

        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getOrCreateDatabasePassphrase(context: Context): String {
        val prefs = getEncryptedSharedPreferences(context)
        var passphrase = prefs.getString(DB_ENCRYPTION_KEY, null)
        if (passphrase == null) {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(DB_ENCRYPTION_KEY, passphrase).apply()
        }
        return passphrase ?: "" // Fix: always return a non-null String
    }

    fun isMasterPasswordSet(context: Context): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(PASSPHRASE_KEY, null) != null
    }

    fun saveMasterPassword(context: Context, password: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putString(PASSPHRASE_KEY, password).apply()
    }

    fun saveMasterPasswordAndEmail(context: Context, password: String, email: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit()
            .putString(PASSPHRASE_KEY, password)
            .putString(EMAIL_KEY, email)
            .apply()
    }

    fun checkMasterPassword(context: Context, password: String): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        val saved = prefs.getString(PASSPHRASE_KEY, null)
        return saved == password
    }

    fun getMasterPassword(context: Context): String {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(PASSPHRASE_KEY, "") ?: ""
    }

    fun saveSecurityAnswers(context: Context, answer1: String, answer2: String, answer3: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit()
            .putString(SECURITY_ANSWER1_KEY, EncryptionUtils.encrypt(answer1.trim().lowercase()))
            .putString(SECURITY_ANSWER2_KEY, EncryptionUtils.encrypt(answer2.trim().lowercase()))
            .putString(SECURITY_ANSWER3_KEY, EncryptionUtils.encrypt(answer3.trim().lowercase()))
            .apply()
    }

    fun checkSecurityAnswers(context: Context, answer1: String, answer2: String, answer3: String): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        val a1 = prefs.getString(SECURITY_ANSWER1_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        val a2 = prefs.getString(SECURITY_ANSWER2_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        val a3 = prefs.getString(SECURITY_ANSWER3_KEY, null)?.let { EncryptionUtils.decrypt(it) }
        return a1 == answer1.trim().lowercase() &&
               a2 == answer2.trim().lowercase() &&
               a3 == answer3.trim().lowercase()
    }

    fun areSecurityAnswersSet(context: Context): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(SECURITY_ANSWER1_KEY, null) != null &&
               prefs.getString(SECURITY_ANSWER2_KEY, null) != null &&
               prefs.getString(SECURITY_ANSWER3_KEY, null) != null
    }

    fun setLoggedIn(context: Context, loggedIn: Boolean) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putBoolean(LOGGED_IN_KEY, loggedIn)
            .putLong(LAST_ACTIVE_KEY, System.currentTimeMillis())
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        val loggedIn = prefs.getBoolean(LOGGED_IN_KEY, false)
        val lastActive = prefs.getLong(LAST_ACTIVE_KEY, 0L)
        val now = System.currentTimeMillis()
        val timeoutMillis = SESSION_TIMEOUT_MINUTES * 60 * 1000
        if (loggedIn && now - lastActive < timeoutMillis) {
            // Aggiorna il timestamp di attività
            prefs.edit().putLong(LAST_ACTIVE_KEY, now).apply()
            return true
        } else {
            // Timeout scaduto o non loggato
            logout(context)
            return false
        }
    }

    fun logout(context: Context) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().clear().apply() // Cancella tutto per sicurezza
    }
}
