package com.arcxp.thearcxptv.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.*

@Entity
@TypeConverters(DateConverter::class)
data class VideoToRemember(
    @PrimaryKey val uuid: String,
    @ColumnInfo val videoTitle: String,
    @ColumnInfo val playPosition: Long,
    @ColumnInfo val playLength: Long,
    @ColumnInfo val thumbnailURL: String,
    @ColumnInfo val createdAt: Date = Date(),
    @ColumnInfo val description: String,
    @ColumnInfo val credit: String,
    @ColumnInfo val displayDate: String,
    @ColumnInfo val resizedURL: String,
    @ColumnInfo val fallback: String
)

@Entity
data class UpdatePosition(
    val playPosition: Long,
    val uuid: String
)