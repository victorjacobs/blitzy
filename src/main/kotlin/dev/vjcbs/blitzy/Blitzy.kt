package dev.vjcbs.blitzy

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val clusteringInterval = 60 * 1000

class Blitzy {

    private val log = logger()

    private val lightningStrikeStorage = LightningStrikeStorage()

    private val client = BlitzOrtungClient(
        Coordinate(56.0, 10.0),
        Coordinate(43.1, 30.0)
        // Square around Berlin
//        Coordinate(53.1, 12.4),
//        Coordinate(51.8, 14.3)
    ) {
        lightningStrikeStorage.add(it)
    }

    fun run() = runBlocking {
        launch {
            client.startAndKeepAlive()
        }

        while (true) {
            delay(clusteringInterval.toLong())

            lightningStrikeStorage.prune()

            val clusters = cluster(lightningStrikeStorage.asArray())

            log.info(
                "Total number of clusters: {}, largest one: {}",
                clusters.size, clusters.minBy { it.numberOfElements }
            )
        }
    }
}

fun main() = Blitzy().run()
