package com.outerspace.luismaps2.repositories

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.outerspace.luismaps2.domain.WorldLocation
import javax.inject.Inject

@Entity(tableName = "Locations")
data class WorldLocationEntity (
    @PrimaryKey(autoGenerate = true) val locationId: Long = 0,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lon") val lon: Double,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_maps_poi") val isMapsPoi: Boolean) {
        constructor(wl: WorldLocation): this(
            lat = wl.lat,
            lon = wl.lon,
            title = wl.title,
            description = wl.description,
            isMapsPoi = wl.isMapsPoi,
        )
}

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

@Database(entities = [WorldLocationEntity::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun worldLocationDao(): WorldLocationDao
}

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
