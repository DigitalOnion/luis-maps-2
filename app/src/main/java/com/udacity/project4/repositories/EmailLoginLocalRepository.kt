package com.udacity.project4.repositories

import javax.inject.Inject





class EmailLoginLocalRepository
    @Inject constructor(private val dao: EmailLoginDao)
{
    fun insert(login: EmailLogin) = dao.insert(login)
    fun delete(targetEmail: String) = dao.delete(targetEmail)
    fun countEmailByName(targetEmail: String): Int = dao.countEmailByName(targetEmail)
    fun passwordMatch(targetEmail: String, targetPassword: String): Boolean = dao.passwordMatch(targetEmail, targetPassword)
    fun updatePassword(targetEmail: String, targetPassword: String, newPassword: String) = dao.updatePassword(targetEmail, targetPassword, newPassword)
}