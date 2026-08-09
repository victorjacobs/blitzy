package dev.vjcbs.blitzy

import dev.vjcbs.blitzy.blitzortung.BlitzortungClientObserver
import dev.vjcbs.blitzy.blitzortung.DiscardReason
import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class BlitzyMetrics(
    lightningStrikeStorage: LightningStrikeStorage,
    val registry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
) : BlitzortungClientObserver {
    private val clustersTracked = AtomicInteger()
    private val clusteredStrikes = AtomicInteger()
    private val largestClusterStrikes = AtomicInteger()
    private val lastSuccessfulClusteringTimestampSeconds = AtomicLong()
    private val websocketConnected = AtomicInteger()

    private val messagesReceived = counter(
        "blitzy.lightning.messages.received",
        "Lightning messages received from Blitzortung"
    )
    private val strikesAccepted = counter(
        "blitzy.lightning.strikes.accepted",
        "Lightning strikes accepted into storage"
    )
    private val strikesPruned = counter(
        "blitzy.lightning.strikes.pruned",
        "Expired lightning strikes removed from storage"
    )
    private val reconnectionAttempts = counter(
        "blitzy.websocket.reconnection.attempts",
        "WebSocket reconnection attempts"
    )
    private val websocketErrors = counter(
        "blitzy.websocket.errors",
        "WebSocket errors"
    )
    private val discardedMessages = DiscardReason.entries.associateWith { reason ->
        Counter.builder("blitzy.lightning.messages.discarded")
            .description("Lightning messages discarded before storage")
            .tag("reason", reason.tagValue)
            .register(registry)
    }
    private val successfulClusteringRuns = clusteringRunsCounter("success")
    private val failedClusteringRuns = clusteringRunsCounter("failure")
    private val clusteringDuration = Timer.builder("blitzy.clustering.duration")
        .description("Time spent clustering lightning strikes")
        .serviceLevelObjectives(
            Duration.ofMillis(100),
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            Duration.ofSeconds(30)
        )
        .register(registry)
    private val clusterSize = DistributionSummary.builder("blitzy.cluster.size")
        .description("Number of lightning strikes in each cluster")
        .baseUnit("strikes")
        .serviceLevelObjectives(25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
        .register(registry)

    init {
        Gauge.builder("blitzy.lightning.strikes.stored", lightningStrikeStorage) { it.size().toDouble() }
            .description("Lightning strikes currently stored")
            .register(registry)
        atomicGauge("blitzy.clusters.tracked", "Clusters currently tracked", clustersTracked)
        atomicGauge("blitzy.clustered.strikes.current", "Stored strikes assigned to tracked clusters", clusteredStrikes)
        atomicGauge("blitzy.cluster.largest.strikes", "Lightning strikes in the largest tracked cluster", largestClusterStrikes)
        atomicGauge(
            "blitzy.clustering.last.success.timestamp.seconds",
            "Unix timestamp of the last successful clustering run",
            lastSuccessfulClusteringTimestampSeconds
        )
        atomicGauge("blitzy.websocket.connected", "Whether the Blitzortung WebSocket is connected", websocketConnected)
    }

    override fun messageReceived() = messagesReceived.increment()

    override fun messageDiscarded(reason: DiscardReason) = discardedMessages.getValue(reason).increment()

    override fun lightningStrikeStored() = strikesAccepted.increment()

    override fun connectionChanged(connected: Boolean) = websocketConnected.set(if (connected) 1 else 0)

    override fun reconnectionAttempted() = reconnectionAttempts.increment()

    override fun websocketError() = websocketErrors.increment()

    fun lightningStrikesPruned(count: Int) {
        strikesPruned.increment(count.toDouble())
    }

    fun clusteringSucceeded(clusters: List<Cluster>, durationNanos: Long) {
        clusteringDuration.record(durationNanos, TimeUnit.NANOSECONDS)
        successfulClusteringRuns.increment()
        clustersTracked.set(clusters.size)
        clusteredStrikes.set(clusters.sumOf { it.numberOfElements })
        largestClusterStrikes.set(clusters.maxOfOrNull { it.numberOfElements } ?: 0)
        lastSuccessfulClusteringTimestampSeconds.set(Instant.now().epochSecond)
        clusters.forEach { clusterSize.record(it.numberOfElements.toDouble()) }
    }

    fun clusteringFailed(durationNanos: Long) {
        clusteringDuration.record(durationNanos, TimeUnit.NANOSECONDS)
        failedClusteringRuns.increment()
    }

    private fun counter(name: String, description: String) = Counter.builder(name)
        .description(description)
        .register(registry)

    private fun clusteringRunsCounter(result: String) = Counter.builder("blitzy.clustering.runs")
        .description("Clustering runs by result")
        .tag("result", result)
        .register(registry)

    private fun atomicGauge(name: String, description: String, value: AtomicInteger) {
        Gauge.builder(name, value) { it.get().toDouble() }
            .description(description)
            .register(registry)
    }

    private fun atomicGauge(name: String, description: String, value: AtomicLong) {
        Gauge.builder(name, value) { it.get().toDouble() }
            .description(description)
            .register(registry)
    }
}

private val prometheusContentType = ContentType.parse("text/plain; version=0.0.4; charset=utf-8")

fun Route.metricsEndpoint(metrics: BlitzyMetrics) {
    get("/metrics") {
        call.respondText(metrics.registry.scrape(), prometheusContentType)
    }
}
