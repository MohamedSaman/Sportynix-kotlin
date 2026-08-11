package com.sportynix.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cleared_cutoffs")
data class ClearedCutoffEntity(
    @PrimaryKey val chatId: Long,
    val cutoffIso: String
)
