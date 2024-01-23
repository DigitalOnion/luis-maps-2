package com.udacity.project4.repositories

import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemindersLocalRepository
    @Inject constructor(private val dao: WorldLocationDao)
    : ReminderDataSource
{
    override suspend fun insert(location: WorldLocationEntity): Long = dao.insert(location)
    override suspend fun getLocations(): List<WorldLocationEntity> = dao.getLocations()
    override suspend fun getLocationAt(poiId: Long): List<WorldLocationEntity> = dao.getLocationAt(poiId)
    override suspend fun deleteLocationAt(poiId: Long) = dao.deleteLocationAt(poiId)
    override suspend fun updateLocationAt(poiId: Long, title: String, description: String) = dao.updateLocationAt(poiId, title, description)
    override suspend fun deleteAll() = dao.deleteAll()

    override suspend fun getReminders(): Result<List<ReminderDTO>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.Success(dao.getLocations())
        } catch (ex: Exception) {
            Result.Error(ex.message)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) { dao.insert(reminder) }

    override suspend fun getReminder(id: String): Result<ReminderDTO> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.Success(dao.getLocationAt(id.toLong()).first())
        } catch (ex: Exception) {
            Result.Error(ex.message)
        }
    }

    override suspend fun deleteAllReminders() = dao.deleteAll()
}
