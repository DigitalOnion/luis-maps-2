package com.outerspace.luismaps2.repositories

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "email_login", indices = [Index(value = ["email_name"], unique = true)])
data class EmailLogin (
    @PrimaryKey(autoGenerate = true) val emailLoginId: Int = 0,
    @ColumnInfo(name = "email_name") val emailName: String,
    @ColumnInfo(name = "password") val password: String,
)