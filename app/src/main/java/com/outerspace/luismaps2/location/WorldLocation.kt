package com.outerspace.luismaps2.location

import com.google.android.gms.maps.model.LatLng

data class WorldLocation(
    var lat: Double,
    var lon: Double)
{
    var title: String = ""
    var description: String = ""
    var valid: Boolean = true

    constructor(lat: Double, lon: Double, title: String, description: String): this(lat, lon) {
        this.title = title
        this.description = description
    }

    constructor(latLng: LatLng): this(latLng.latitude, latLng.longitude)

    constructor(entity: WorldLocationEntity): this(entity.lat, entity.lon, entity.title, entity.description)

    fun getLatLng(): LatLng {return LatLng(lat, lon) }

    override fun toString(): String = "($lat, $lon)"
//    override fun equals(other: Any?): Boolean {
//        return other != null && this.lat == (other as WorldLocation).lat && this.lon == other.lon
//    }
}