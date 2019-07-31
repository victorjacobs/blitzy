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

class Main {

    private val log = logger()

    private val lightningStrikeStorage = LightningStrikeStorage(Configuration.lightningStrikeTtl)

    private val blitzOrtungClient = BlitzOrtungClient(
        Configuration.topLeftCoordinate,
        Configuration.bottomRightCoordinate
    ) {
        lightningStrikeStorage.add(it)
    }

    private var clusters: List<Cluster> = listOf()

    private var geoJson: FeatureCollection = FeatureCollection.fromClusters(listOf())

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
                    call.respond(it)
                }
            }
        }.start()

        while (true) {
            delay(Configuration.clusteringInterval)

            lightningStrikeStorage.prune()

            try {
                clusters = cluster(lightningStrikeStorage.asArray())
                geoJson = FeatureCollection.fromClusters(clusters)
            } catch (e: Exception) {
                log.error("Clustering failed", e)
            }

            log.info(
                "Total number of clusters: {}, largest one: {}",
                clusters.size, clusters.minBy { it.numberOfElements }
            )
        }
    }
}

fun main() = Main().run()
