package dev.vjcbs.blitzy

data class Coordinate(
    val lat: Double,
    val lon: Double
) {

    companion object {
        fun fromArray(array: DoubleArray) = Coordinate(
            array[0],
            array[1]
        )
    }

    operator fun plus(other: Coordinate) = Coordinate(
        lat + other.lat,
        lon + other.lon
    )

    operator fun div(div: Int) = Coordinate(
        lat / div,
        lon / div
    )

    override fun toString() = "($lat, $lon)"
}

data class LightningStrike(
    val timestampNanos: Long,
    val coordinate: Coordinate
)

data class Cluster(
    val center: Coordinate,
    val numberOfElements: Int
)
