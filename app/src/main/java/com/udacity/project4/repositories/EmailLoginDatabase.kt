package com.udacity.project4.repositories

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EmailLogin::class], version = 1)
abstract class EmailLoginDatabase: RoomDatabase() {
    abstract fun emailLoginDao(): EmailLoginDao
}
