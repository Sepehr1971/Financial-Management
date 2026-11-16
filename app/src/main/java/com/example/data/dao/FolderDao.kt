package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM custom_folders ORDER BY id ASC")
    fun getAllFoldersFlow(): Flow<List<CustomFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: CustomFolderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(folders: List<CustomFolderEntity>)

    @Delete
    suspend fun delete(folder: CustomFolderEntity)

    @Query("DELETE FROM custom_folders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_folders WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("DELETE FROM custom_folders")
    suspend fun clearAll()
}
