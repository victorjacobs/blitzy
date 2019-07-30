package dev.vjcbs.blitzy

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN
import de.lmu.ifi.dbs.elki.data.DoubleVector
import de.lmu.ifi.dbs.elki.data.type.TypeUtil
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection
import de.lmu.ifi.dbs.elki.distance.distancefunction.geo.LatLngDistanceFunction
import de.lmu.ifi.dbs.elki.math.geodesy.SphericalHaversineEarthModel

fun cluster(data: Array<DoubleArray>): List<Cluster> {
    val dbc = ArrayAdapterDatabaseConnection(data)
    val db = StaticArrayDatabase(dbc, null)
    db.initialize()

    val clusteringResult = DBSCAN(
        LatLngDistanceFunction(SphericalHaversineEarthModel.STATIC),
        5000.0,
        10
    ).run(db)

    val relation = db.getRelation<DoubleVector>(TypeUtil.DOUBLE_VECTOR_FIELD)

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
