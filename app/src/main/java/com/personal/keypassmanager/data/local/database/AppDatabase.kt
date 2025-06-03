package com.personal.keypassmanager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.keypassmanager.data.local.DatabasePassphraseProvider
import com.personal.keypassmanager.data.local.dao.CredentialDao
import com.personal.keypassmanager.data.model.Credential

// Database Room principale per la gestione delle credenziali cifrate.
@Database(entities = [Credential::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Restituisce il DAO per le credenziali
    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbPassphrase = DatabasePassphraseProvider.getOrCreateDatabasePassphrase(context)
                val factory = DatabasePassphraseProvider.getSupportFactory(dbPassphrase)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keypass_encrypted.db"
                )
                    .openHelperFactory(factory)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
