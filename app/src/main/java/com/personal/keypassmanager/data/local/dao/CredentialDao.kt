package com.personal.keypassmanager.data.local.dao

import androidx.room.*
import androidx.room.Dao
import com.personal.keypassmanager.data.model.Credential
import kotlinx.coroutines.flow.Flow

// DAO Room per la gestione CRUD delle credenziali.
@Dao
interface CredentialDao {
    // Inserisce una credenziale
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: Credential)

    // Aggiorna una credenziale
    @Update
    suspend fun updateCredential(credential: Credential)

    // Elimina una credenziale
    @Delete
    suspend fun deleteCredential(credential: Credential)

    // Restituisce tutte le credenziali ordinate per company
    @Query("SELECT * FROM credentials ORDER BY company ASC")
    fun getAllCredentials(): Flow<List<Credential>>

    // Restituisce una credenziale per id
    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: Int): Credential?

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()
}
