package com.personal.keypassmanager.data.local

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

object DatabasePassphraseProvider {

    private const val PREFS_NAME = "encrypted_prefs"
    private const val PASSPHRASE_KEY = "db_passphrase"
    private const val EMAIL_KEY = "user_email"
    private const val RESET_CODE_KEY = "reset_code"
    private const val RESET_CODE_EXPIRY_KEY = "reset_code_expiry"

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
        var passphrase = prefs.getString(PASSPHRASE_KEY, null)
        if (passphrase == null) {
            val random = SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            passphrase = Base64.encodeToString(bytes, Base64.NO_WRAP)
            prefs.edit().putString(PASSPHRASE_KEY, passphrase).apply()
        }
        return passphrase
    }

    fun isMasterPasswordSet(context: Context): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(PASSPHRASE_KEY, null) != null
    }

    fun saveMasterPassword(context: Context, password: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putString(PASSPHRASE_KEY, password).apply()
    }

    fun checkMasterPassword(context: Context, password: String): Boolean {
        val prefs = getEncryptedSharedPreferences(context)
        val saved = prefs.getString(PASSPHRASE_KEY, null)
        return saved == password
    }

    fun saveUserEmail(context: Context, email: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putString(EMAIL_KEY, email).apply()
    }

    fun getUserEmail(context: Context): String? {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(EMAIL_KEY, null)
    }

    fun saveResetCode(context: Context, code: String, expiry: Long) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().putString(RESET_CODE_KEY, code)
            .putLong(RESET_CODE_EXPIRY_KEY, expiry)
            .apply()
    }

    fun getResetCode(context: Context): Pair<String?, Long> {
        val prefs = getEncryptedSharedPreferences(context)
        return prefs.getString(RESET_CODE_KEY, null) to prefs.getLong(RESET_CODE_EXPIRY_KEY, 0L)
    }

    fun clearResetCode(context: Context) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit().remove(RESET_CODE_KEY).remove(RESET_CODE_EXPIRY_KEY).apply()
    }

    fun sendResetEmailIntent(context: Context, email: String, code: String) {
        val subject = "Codice di recupero KeyPassManager"
        val body = "Il tuo codice di recupero è: $code\nValido per 10 minuti."
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun saveMasterPasswordAndEmail(context: Context, password: String, email: String) {
        val prefs = getEncryptedSharedPreferences(context)
        prefs.edit()
            .putString(PASSPHRASE_KEY, password)
            .putString(EMAIL_KEY, email)
            .apply()
    }

    suspend fun sendResetEmailSmtp(
        smtpHost: String,
        smtpPort: String,
        smtpUser: String,
        smtpPassword: String,
        toEmail: String,
        code: String
    ) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", smtpHost)
            put("mail.smtp.port", smtpPort)
        }
        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(smtpUser, smtpPassword)
            }
        })
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(smtpUser))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            subject = "Codice di recupero KeyPassManager"
            setText("Il tuo codice di recupero è: $code\nValido per 10 minuti.")
        }
        withContext(Dispatchers.IO) {
            Transport.send(message)
        }
    }
}
