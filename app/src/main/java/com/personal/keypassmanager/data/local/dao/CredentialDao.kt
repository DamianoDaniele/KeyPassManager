package com.personal.keypassmanager.data.local.dao

import androidx.room.*
import androidx.room.Dao
import com.personal.keypassmanager.data.model.Credential
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: Credential)

    @Update
    suspend fun updateCredential(credential: Credential)

    @Delete
    suspend fun deleteCredential(credential: Credential)

    @Query("SELECT * FROM credentials ORDER BY company ASC")
    fun getAllCredentials(): Flow<List<Credential>>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getCredentialById(id: Int): Credential?
}
