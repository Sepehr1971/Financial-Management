package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_folders")
data class CustomFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: Long = 0xFF10B981,
    val isAnnualFolder: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
