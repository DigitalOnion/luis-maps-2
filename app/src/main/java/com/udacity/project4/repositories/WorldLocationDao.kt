package com.udacity.project4.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorldLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: WorldLocationEntity): Long

    @Query("select * from Locations")
    suspend fun getLocations(): List<WorldLocationEntity>

    @Query("select * from Locations where locationId = :poiId")
    suspend fun getLocationAt(poiId: Long): List<WorldLocationEntity>

    @Query("delete from Locations where locationId = :poiId")
    suspend fun deleteLocationAt(poiId: Long)

    @Query("update Locations set title = :title, description = :description where locationId = :poiId")
    suspend fun updateLocationAt(poiId: Long, title: String, description: String)

    @Query("delete from Locations")
    suspend fun deleteAll()
}