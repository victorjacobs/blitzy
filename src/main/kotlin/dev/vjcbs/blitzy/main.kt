package dev.vjcbs.blitzy

import com.fasterxml.jackson.module.kotlin.KotlinModule
import dev.vjcbs.blitzy.blitzortung.BlitzortungClient
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.suppressCompression
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class Main {
    private val log = logger()

    private val lightningStrikeStorage = LightningStrikeStorage(Configuration.lightningStrikeTtl)

    private var clusters: List<Cluster> = listOf()

    private var geoJson: FeatureCollection = FeatureCollection.fromClusters(listOf())

    fun run() = runBlocking {
        log.info("Configuration: $Configuration")

        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        lateinit var metrics: BlitzyMetrics

        val server = embeddedServer(
            Netty,
            port = Configuration.listenPort,
            host = Configuration.listenAddress
        ) {
            install(Compression)
            install(MicrometerMetrics) {
                this.registry = registry
            }
            metrics = BlitzyMetrics(lightningStrikeStorage, registry)
            install(ContentNegotiation) {
                jackson {
                    registerModule(KotlinModule.Builder().build())
                }
            }

            routing {
                get("/blitzortung.geojson") {
                    call.suppressCompression()
                    call.respond(geoJson)
                }

                metricsEndpoint(metrics)
            }
        }

        server.start()

        val blitzortungClient = BlitzortungClient(
            Configuration.topLeftCoordinate,
            Configuration.bottomRightCoordinate,
            metrics
        ) {
            lightningStrikeStorage.add(it)
        }

        launch {
            blitzortungClient.startAndKeepAlive()
        }

        while (true) {
            delay(Configuration.clusteringInterval)

            metrics.lightningStrikesPruned(lightningStrikeStorage.prune())

            val clusteringStartNanos = System.nanoTime()
            try {
                clusters = cluster(lightningStrikeStorage.asArray())
                geoJson = FeatureCollection.fromClusters(clusters)
                metrics.clusteringSucceeded(clusters, System.nanoTime() - clusteringStartNanos)
            } catch (e: Exception) {
                metrics.clusteringFailed(System.nanoTime() - clusteringStartNanos)
                log.error("Clustering failed", e)
            }

            log.info(
                "Total number of clusters: ${clusters.size}, largest one: ${clusters.minByOrNull { it.numberOfElements }}"
            )
        }
    }
}

fun main() = Main().run()
