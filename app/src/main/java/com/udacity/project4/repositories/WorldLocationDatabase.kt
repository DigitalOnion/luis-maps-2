package com.udacity.project4.repositories

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [WorldLocationEntity::class], version = 1)
abstract class WorldLocationDatabase : RoomDatabase() {
    abstract fun worldLocationDao(): WorldLocationDao
}
