package dev.vjcbs.blitzy

import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.vjcbs.blitzy.blitzortung.BlitzortungClient
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Main {
    private val log = logger()

    private val lightningStrikeStorage = LightningStrikeStorage(Configuration.lightningStrikeTtl)

    private val blitzortungClient = BlitzortungClient(
        Configuration.topLeftCoordinate,
        Configuration.bottomRightCoordinate
    ) {
        lightningStrikeStorage.add(it)
    }

    private var clusters: List<Cluster> = listOf()

    private var geoJson: FeatureCollection = FeatureCollection.fromClusters(listOf())

    fun run() = runBlocking {
        log.info("Configuration: $Configuration")

        launch {
            blitzortungClient.startAndKeepAlive()
        }

        embeddedServer(Netty, 8080) {
            install(Compression)
            install(ContentNegotiation) {
                jackson {
                    registerModule(KotlinModule.Builder().build())
                }
            }

            routing {
                get("/blitzortung.geojson") {
                    call.respond(geoJson)
                }

                get("/metrics") {
                    call.respond(mapOf(
                        "clusters" to clusters.size,
                        "strikes" to lightningStrikeStorage.size()
                    ))
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
                "Total number of clusters: ${clusters.size}, largest one: ${clusters.minByOrNull { it.numberOfElements }}"
            )
        }
    }
}

fun main() = Main().run()
