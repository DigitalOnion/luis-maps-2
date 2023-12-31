package com.outerspace.luismaps2.repositories

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.outerspace.luismaps2.domain.WorldLocation

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