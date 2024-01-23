package com.udacity.project4.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

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