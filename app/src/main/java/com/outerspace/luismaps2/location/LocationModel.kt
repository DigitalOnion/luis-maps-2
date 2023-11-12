package com.outerspace.luismaps2.location

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "Locations")
data class WorldLocationEntity (
    @PrimaryKey(autoGenerate = true) val locationId: Int = 0,
    @ColumnInfo(name = "lat") val lat: Double,
    @ColumnInfo(name = "lon") val lon: Double,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String) {
        constructor(wl: WorldLocation): this(
            lat = wl.lat,
            lon = wl.lon,
            title = wl.title,
            description = wl.description)
}

@Dao
interface WorldLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(location: WorldLocationEntity)

    @Query("select * from Locations")
    fun getLocations(): List<WorldLocationEntity>

    @Query("delete from Locations where lat = :latitude and lon = :longitude")
    fun deleteLocationAt(latitude: Double, longitude: Double)

    @Query("delete from Locations")
    fun deleteAll()
}

@Database(entities = [WorldLocationEntity::class], version = 1)
abstract class LocationDatabase : RoomDatabase() {
    abstract fun worldLocationDao(): WorldLocationDao
}

