package dev.vjcbs.blitzy.blitzortung

import dev.vjcbs.blitzy.Coordinate
import dev.vjcbs.blitzy.LightningStrike

data class BlitzortungLightningStrike(
    val time: Long,
    val lat: Double,
    val lon: Double
) {
    fun toLightningStrike() = LightningStrike(
        timestampNanos = time,
        coordinate = Coordinate(lat, lon)
    )
}
