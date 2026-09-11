package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT mediaId FROM favorites")
    fun getFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaId = :mediaId")
    suspend fun removeFavorite(mediaId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)")
    suspend fun isFavorite(mediaId: Long): Boolean
}

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllTrash(): Flow<List<TrashEntity>>

    @Query("SELECT mediaId FROM trash_items")
    fun getTrashIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun moveToTrash(item: TrashEntity)

    @Query("DELETE FROM trash_items WHERE mediaId = :mediaId")
    suspend fun restoreFromTrash(mediaId: Long)

    @Query("DELETE FROM trash_items WHERE mediaId = :mediaId")
    suspend fun deletePermanently(mediaId: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()
}

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY addedToVaultAt DESC")
    fun getAllVaultItems(): Flow<List<VaultEntity>>

    @Query("SELECT mediaId FROM vault_items")
    fun getVaultMediaIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToVault(item: VaultEntity)

    @Query("DELETE FROM vault_items WHERE mediaId = :mediaId")
    suspend fun removeFromVault(mediaId: Long)

    @Query("SELECT * FROM vault_items WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getVaultItem(mediaId: Long): VaultEntity?
}

@Database(
    entities = [FavoriteEntity::class, TrashEntity::class, VaultEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun trashDao(): TrashDao
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "my_gallery_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
