package com.outerspace.luismaps2.domain

import com.google.android.gms.maps.model.LatLng
import com.outerspace.luismaps2.repositories.WorldLocationEntity

data class WorldLocation(
    var lat: Double,
    var lon: Double)
{
    var id: Long = 0
    var title: String = ""
    var description: String = ""
    var isMapsPoi: Boolean = false

    constructor(lat: Double, lon: Double, title: String, description: String, id:Long = 0): this(lat, lon) {
        this.title = title
        this.description = description
        this.id = id
        isMapsPoi = false
    }

    constructor(lat: Double, lon: Double, title: String, description: String, isMapsPoi: Boolean, id:Long = 0): this(lat, lon, title, description, id) {
        this.isMapsPoi = isMapsPoi
    }

    constructor(latLng: LatLng): this(latLng.latitude, latLng.longitude)

    constructor(entity: WorldLocationEntity): this(entity.lat, entity.lon, entity.title, entity.description, entity.isMapsPoi, entity.locationId)

    fun getLatLng(): LatLng {return LatLng(lat, lon) }

    override fun toString(): String = "($lat, $lon)"
}