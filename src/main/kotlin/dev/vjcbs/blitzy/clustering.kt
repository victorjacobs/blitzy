package dev.vjcbs.blitzy

import elki.clustering.dbscan.DBSCAN
import elki.data.NumberVector
import elki.data.type.TypeUtil
import elki.database.StaticArrayDatabase
import elki.datasource.ArrayAdapterDatabaseConnection
import elki.distance.geo.LatLngDistance
import elki.math.geodesy.SphericalHaversineEarthModel
import org.slf4j.LoggerFactory

fun cluster(data: Array<DoubleArray>): List<Cluster> {
    if (data.isEmpty()) {
        return listOf()
    }

    val log = LoggerFactory.getLogger(::cluster.javaClass)

    val dbc = ArrayAdapterDatabaseConnection(data)
    val db = StaticArrayDatabase(dbc, null)
    db.initialize()
    val relation = db.getRelation<NumberVector>(TypeUtil.NUMBER_VECTOR_FIELD)

    val clusteringStartTimestamp = System.currentTimeMillis()

    val clusteringResult = DBSCAN(
        LatLngDistance(SphericalHaversineEarthModel.STATIC),
        Configuration.clusteringEps,
        Configuration.clusteringMinPts
    ).run(relation)

    log.info("Clustering took ${System.currentTimeMillis() - clusteringStartTimestamp}ms")

    return clusteringResult.allClusters.filter {
        !it.isNoise
    }.map { cluster ->
        var average = Coordinate(0.0, 0.0)

        cluster.iDs.forEach { id ->
            val point = relation.get(id).toArray()

            average += Coordinate.fromArray(point)
        }

        Cluster(
            average / cluster.iDs.size(),
            cluster.iDs.size()
        )
    }
}
