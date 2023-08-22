package com.arcxp.thearcxptv.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VideoToRemember::class],
    version = 1
)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun rememberVideoDao(): RememberVideoDao
}