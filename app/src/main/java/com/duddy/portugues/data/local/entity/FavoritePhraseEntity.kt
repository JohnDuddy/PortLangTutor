package com.duddy.portugues.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_phrase")
data class FavoritePhraseEntity(
    @PrimaryKey
    @ColumnInfo(name = "phrase_id")
    val phraseId: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,
)
