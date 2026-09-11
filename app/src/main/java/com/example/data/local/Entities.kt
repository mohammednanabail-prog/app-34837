package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trash_items")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val mimeType: String,
    val mediaType: String,
    val deletedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_items")
data class VaultEntity(
    @PrimaryKey val mediaId: Long,
    val originalUriString: String,
    val vaultFilePath: String,
    val name: String,
    val dateTaken: Long,
    val size: Long,
    val mimeType: String,
    val mediaType: String,
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val addedToVaultAt: Long = System.currentTimeMillis()
)

