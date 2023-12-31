package com.outerspace.luismaps2.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorldLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(location: WorldLocationEntity): Long

    @Query("select * from Locations")
    fun getLocations(): List<WorldLocationEntity>

    @Query("select * from Locations where locationId = :poiId")
    fun getLocationAt(poiId: Long): List<WorldLocationEntity>

    @Query("delete from Locations where locationId = :poiId")
    fun deleteLocationAt(poiId: Long)

    @Query("update Locations set title = :title, description = :description where locationId = :poiId")
    fun updateLocationAt(poiId: Long, title: String, description: String)

    @Query("delete from Locations")
    fun deleteAll()
}