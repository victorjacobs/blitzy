package dev.vjcbs.blitzy

import dev.vjcbs.blitzy.blitzortung.DiscardReason
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BlitzyMetricsTest {
    @Test
    fun `exposes application metrics in Prometheus format`() = testApplication {
        val storage = LightningStrikeStorage(60_000)
        val metrics = BlitzyMetrics(storage)

        storage.add(LightningStrike(System.nanoTime(), Coordinate(50.0, 4.0)))
        metrics.lightningStrikeStored()
        metrics.messageReceived()
        metrics.messageDiscarded(DiscardReason.OUTSIDE_MONITORED_AREA)
        metrics.clusteringSucceeded(
            listOf(
                Cluster(Coordinate(50.0, 4.0), 30),
                Cluster(Coordinate(51.0, 5.0), 50)
            ),
            250_000_000
        )

        application {
            routing {
                metricsEndpoint(metrics)
            }
        }

        val response = client.get("/metrics")
        val body = response.bodyAsText()

        assertEquals(200, response.status.value)
        assertTrue(response.headers["Content-Type"]!!.startsWith("text/plain"))
        assertTrue(body.contains("blitzy_lightning_strikes_stored 1.0"))
        assertTrue(body.contains("blitzy_lightning_strikes_accepted_total 1.0"))
        assertTrue(body.contains("blitzy_clusters_tracked 2.0"))
        assertTrue(body.contains("blitzy_clustered_strikes_current 80.0"))
        assertTrue(body.contains("blitzy_cluster_largest_strikes 50.0"))
        assertTrue(body.contains("blitzy_lightning_messages_discarded_total{reason=\"outside_monitored_area\"} 1.0"))
        assertTrue(body.contains("blitzy_clustering_runs_total{result=\"success\"} 1.0"))
    }
}
