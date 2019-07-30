package dev.vjcbs.blitzy

import java.util.UUID

data class FeatureCollection(
    val features: List<Feature>,
    val metadata: Metadata,
    val type: String = "FeatureCollection"
) {
    companion object {
        fun fromClusters(clusters: List<Cluster>): FeatureCollection {
            val features = clusters.map {
                Feature(
                    UUID.randomUUID().toString(),
                    PointGeometry.fromCoordinate(it.center),
                    mapOf(
                        "size" to it.numberOfElements.toString()
                    )
                )
            }

            val metadata = Metadata(
                clusters.size
            )

            return FeatureCollection(
                features,
                metadata
            )
        }
    }
}

data class Metadata(
    val count: Int,
    val title: String = "blitzortung.org thunderstorms"
)

data class Feature(
    val id: String,
    val geometry: PointGeometry,
    val properties: Map<String, String> = mapOf(),
    val type: String = "Feature"
)

data class PointGeometry(
    val coordinates: List<Double>,
    val type: String = "Point"
) {
    companion object {
        fun fromCoordinate(coordinate: Coordinate) = PointGeometry(
            listOf(coordinate.lon, coordinate.lat)
        )
    }
}
