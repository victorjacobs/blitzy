package dev.vjcbs.blitzy

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Blitzy {

    private val clusteringInterval = 60 * 1000

    private val log = logger()

    private val lightningStrikeStorage = LightningStrikeStorage()

    private val blitzOrtungClient = BlitzOrtungClient(
        Coordinate(56.0, 10.0),
        Coordinate(43.1, 30.0)
        // Square around Berlin
//        Coordinate(53.1, 12.4),
//        Coordinate(51.8, 14.3)
    ) {
        lightningStrikeStorage.add(it)
    }

    private var clusters: List<Cluster> = listOf()

    private var geoJson: FeatureCollection? = null

    fun run() = runBlocking {
        launch {
            blitzOrtungClient.startAndKeepAlive()
        }

        embeddedServer(Netty, 8080) {
            install(Compression)
            install(ContentNegotiation) {
                jackson {
                    registerModule(KotlinModule())
                }
            }

            routing {
                get("/blitzortung.geojson") {
                    geoJson?.let {
                        call.respond(it)
                    }
                }
            }
        }.start()

        while (true) {
            delay(clusteringInterval.toLong())

            lightningStrikeStorage.prune()

            clusters = cluster(lightningStrikeStorage.asArray())
            geoJson = FeatureCollection.fromClusters(clusters)

            log.info(
                "Total number of clusters: {}, largest one: {}",
                clusters.size, clusters.minBy { it.numberOfElements }
            )
        }
    }
}

fun main() = Blitzy().run()
