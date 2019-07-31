package dev.vjcbs.blitzy

object Configuration {

    // Top left and bottom right coordinates of the area monitored. Defaults to roughly Europe.
    val topLeftCoordinate = getFromEnv("TOP_LEFT_COORDINATE")?.let {
        Coordinate.fromString(it)
    } ?: Coordinate(63.14, -18.11)

    val bottomRightCoordinate = getFromEnv("BOTTOM_RIGHT_COORDINATE")?.let {
        Coordinate.fromString(it)
    } ?: Coordinate(30.54, 22.89)

    // Time between clustering runs in milliseconds, defaults to 1 minute
    val clusteringInterval = getFromEnv("CLUSTERING_INTERVAL")?.toLong() ?: 60 * 1000

    // TTL for lightning strikes, how long are they kept in memory. Defaults to 10 minutes
    val lightningStrikeTtl = getFromEnv("LIGHTNING_STRIKE_TTL")?.toInt() ?: 10 * 60 * 1000

    // Epsilon for DBSCAN. Distance between lightning strikes (in meters) to consider them part of the same cluster.
    // Defaults to 10000.0m
    val clusteringEps = getFromEnv("CLUSTERING_EPS")?.toDouble() ?: 10000.0

    // Minpts for DBSCAN. Minimum number of lightning strikes required to consider it a cluster. Defaults to 25
    val clusteringMinPts = getFromEnv("CLUSTERING_MIN_PTS")?.toInt() ?: 25

    private fun getFromEnv(varName: String): String? = System.getenv(varName)

    private fun getFromEnvOrThrow(varName: String) =
        getFromEnv(varName) ?: throw IllegalStateException("$varName not set")
}
