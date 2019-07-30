package dev.vjcbs.blitzy

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN
import de.lmu.ifi.dbs.elki.data.DoubleVector
import de.lmu.ifi.dbs.elki.data.type.TypeUtil
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LatLngDistanceFunction
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalHaversineEarthModel
import org.slf4j.LoggerFactory

fun cluster(data: Array<DoubleArray>): List<Cluster> {
    val log = LoggerFactory.getLogger(::cluster.javaClass)

    val dbc = ArrayAdapterDatabaseConnection(data)
    val db = StaticArrayDatabase(dbc, null)
    db.initialize()
    val relation = db.getRelation<DoubleVector>(TypeUtil.DOUBLE_VECTOR_FIELD)

    val clusteringStartTimestamp = System.currentTimeMillis()

    val clusteringResult = DBSCAN(
        LatLngDistanceFunction(SphericalHaversineEarthModel.STATIC),
        5000.0,
        10
    ).run(db)

    log.info("Clustering took {}ms", System.currentTimeMillis() - clusteringStartTimestamp)

    return clusteringResult.allClusters.filter {
        !it.isNoise
    }.map { cluster ->
        var average = Coordinate(0.0, 0.0)

        cluster.iDs.forEach { id ->
            val point = (relation.get(id) as DoubleVector).toArray()

            average += Coordinate.fromArray(point)
        }

        Cluster(
            average / cluster.iDs.size(),
            cluster.iDs.size()
        )
    }
}
