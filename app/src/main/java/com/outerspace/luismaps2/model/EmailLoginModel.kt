package com.outerspace.luismaps2.model

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "email_login", indices = [Index(value = ["email_name"], unique = true)])
data class EmailLogin (
    @PrimaryKey(autoGenerate = true) val emailLoginId: Int = 0,
    @ColumnInfo(name = "email_name") val emailName: String,
    @ColumnInfo(name = "password") val password: String,
)

@Dao
interface EmailLoginDao {
    @Insert
    fun insert(login: EmailLogin)

    @Query("select * from email_login where email_name = :targetEmail")
    fun getEmailLogin(targetEmail: String): EmailLogin

//    @Query("select :targetPassword = password as passwordMatches from email_login where email_name = :targetEmail")
//    fun doesPasswordMatch(targetEmail: String, targetPassword: String)
}

@Database(entities = [EmailLogin::class], version = 1)
abstract class EmailLoginDatabase: RoomDatabase() {
    abstract fun emailLoginDao(): EmailLoginDao
}
