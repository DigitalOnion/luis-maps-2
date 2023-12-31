package com.outerspace.luismaps2.repositories

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import javax.inject.Inject

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

    @Query("delete from email_login where email_name = :targetEmail")
    fun delete(targetEmail: String)

    @Query("select count(*) from email_login where email_name = :targetEmail")
    fun countEmailByName(targetEmail: String): Int

    @Query("select count(*) > 0 as passwordMatches from email_login where email_name = :targetEmail and password = :targetPassword")
    fun passwordMatch(targetEmail: String, targetPassword: String): Boolean

    @Query("update email_login set password = :newPassword where email_name = :targetEmail and password = :targetPassword")
    fun updatePassword(targetEmail: String, targetPassword: String, newPassword: String)
}

@Database(entities = [EmailLogin::class], version = 1)
abstract class EmailLoginDatabase: RoomDatabase() {
    abstract fun emailLoginDao(): EmailLoginDao
}

class EmailLoginLocalRepository
    @Inject constructor(private val dao: EmailLoginDao)
{
    fun insert(login: EmailLogin) = dao.insert(login)
    fun delete(targetEmail: String) = dao.delete(targetEmail)
    fun countEmailByName(targetEmail: String): Int = dao.countEmailByName(targetEmail)
    fun passwordMatch(targetEmail: String, targetPassword: String): Boolean = dao.passwordMatch(targetEmail, targetPassword)
    fun updatePassword(targetEmail: String, targetPassword: String, newPassword: String) = dao.updatePassword(targetEmail, targetPassword, newPassword)
}