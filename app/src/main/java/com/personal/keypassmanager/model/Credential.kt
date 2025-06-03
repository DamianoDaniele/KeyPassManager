package com.personal.keypassmanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Entità Room per la credenziale (persistenza locale)
@Entity(tableName = "credentials")
data class Credential(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val company: String,
    val username: String,
    val password: String
)
