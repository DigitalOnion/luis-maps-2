package com.udacity.project4.domain

import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.repositories.WorldLocationEntity

data class WorldLocation(
    var lat: Double,
    var lon: Double)
{
    var id: Long = 0
    var title: String = ""
    var description: String = ""
    var isMapsPoi: Boolean = false

    constructor(id: Long, lat: Double, lon: Double, title: String, description: String): this(lat, lon) {
        this.id = id
        this.title = title
        this.description = description
        isMapsPoi = false
    }

    constructor(id:Long, lat: Double, lon: Double, title: String, description: String, isMapsPoi: Boolean): this(id, lat, lon, title, description) {
        this.isMapsPoi = isMapsPoi
    }

    constructor(latLng: LatLng): this(latLng.latitude, latLng.longitude)

    constructor(entity: WorldLocationEntity): this(entity.locationId, entity.lat, entity.lon, entity.title, entity.description, entity.isMapsPoi)

    fun getLatLng(): LatLng {return LatLng(lat, lon) }

    override fun toString(): String = "($lat, $lon)"

}