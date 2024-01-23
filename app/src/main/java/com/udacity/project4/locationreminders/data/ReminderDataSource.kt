package com.udacity.project4.locationreminders.data

import com.udacity.project4.repositories.ReminderDTO
import com.udacity.project4.repositories.WorldLocationEntity

/**
 * Main entry point for accessing reminders data.
 */
interface ReminderDataSource {
    // Functions designed for the project
    suspend fun insert(location: WorldLocationEntity): Long
    suspend fun getLocations(): List<WorldLocationEntity>
    suspend fun getLocationAt(poiId: Long): List<WorldLocationEntity>
    suspend fun deleteLocationAt(poiId: Long)
    suspend fun updateLocationAt(poiId: Long, title: String, description: String)
    suspend fun deleteAll()

    // Udacity proposed functions
    suspend fun getReminders(): Result<List<ReminderDTO>>
    suspend fun saveReminder(reminder: ReminderDTO)
    suspend fun getReminder(id: String): Result<ReminderDTO>
    suspend fun deleteAllReminders()
}