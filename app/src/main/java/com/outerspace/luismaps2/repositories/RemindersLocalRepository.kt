package com.outerspace.luismaps2.repositories

import androidx.room.Database
import androidx.room.RoomDatabase
import javax.inject.Inject

class RemindersLocalRepository
    @Inject constructor(private val dao: WorldLocationDao)
{
    fun insert(location: WorldLocationEntity): Long = dao.insert(location)
    fun getLocations(): List<WorldLocationEntity> = dao.getLocations()
    fun getLocationAt(poiId: Long): List<WorldLocationEntity> = dao.getLocationAt(poiId)
    fun deleteLocationAt(poiId: Long) = dao.deleteLocationAt(poiId)
    fun updateLocationAt(poiId: Long, title: String, description: String) = dao.updateLocationAt(poiId, title, description)
    fun deleteAll() = dao.deleteAll()
}
