package dev.vjcbs.blitzy

import java.util.UUID

data class FeatureCollection(
    val features: List<Feature>,
    val metadata: FeatureCollectionMetadata
) {
    @Suppress("unused")
    val type
        get() = "FeatureCollection"

    companion object {
        fun fromClusters(clusters: List<Cluster>): FeatureCollection {
            val features = clusters.map { cluster ->
                Feature(
                    UUID.randomUUID().toString(),
                    PointGeometry.fromCoordinate(cluster.center),
                    FeatureProperties(
                        "Thunderstorm",
                        cluster.numberOfElements
                    )
                )
            }

            val metadata = FeatureCollectionMetadata(
                clusters.size
            )

            return FeatureCollection(
                features,
                metadata
            )
        }
    }
}

data class FeatureCollectionMetadata(
    val count: Int
) {
    @Suppress("unused")
    val title
        get() = "blitzortung.org thunderstorms"
}

data class Feature(
    val id: String,
    val geometry: PointGeometry,
    val properties: FeatureProperties
) {
    @Suppress("unused")
    val type
        get() = "Feature"
}

data class FeatureProperties(
    val title: String,
    val size: Int
)

data class PointGeometry(
    val coordinates: List<Double>
) {

    @Suppress("unused")
    val type
        get() = "Point"

    companion object {
        fun fromCoordinate(coordinate: Coordinate) = PointGeometry(
            listOf(coordinate.lon, coordinate.lat)
        )
    }
}
