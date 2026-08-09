package dev.vjcbs.blitzy.blitzortung

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import dev.vjcbs.blitzy.Coordinate
import dev.vjcbs.blitzy.LightningStrike
import dev.vjcbs.blitzy.logger
import kotlinx.coroutines.delay
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

enum class DiscardReason(val tagValue: String) {
    EMPTY_MESSAGE("empty_message"),
    INVALID_MESSAGE("invalid_message"),
    OUTSIDE_MONITORED_AREA("outside_monitored_area")
}

interface BlitzortungClientObserver {
    fun messageReceived() {}

    fun messageDiscarded(reason: DiscardReason) {}

    fun lightningStrikeStored() {}

    fun connectionChanged(connected: Boolean) {}

    fun reconnectionAttempted() {}

    fun websocketError() {}
}

private object NoOpBlitzortungClientObserver : BlitzortungClientObserver

class BlitzortungClient(
    private val topLeft: Coordinate,
    private val bottomRight: Coordinate,
    private val observer: BlitzortungClientObserver = NoOpBlitzortungClientObserver,
    private val onLightningStrike: (LightningStrike) -> Unit
) : WebSocketClient(
    URI("wss://ws1.blitzortung.org")
) {
    private val endpoints = listOf(
        "wss://ws1.blitzortung.org",
        "wss://ws7.blitzortung.org",
        "wss://ws8.blitzortung.org"
    ).map { URI(it) }

    private val log = logger()

    private var currentServerIndex = 1

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    suspend fun startAndKeepAlive() {
        connect()

        while (true) {
            delay(1000)

            if (isClosed) {
                do {
                    log.info("Reconnecting")
                    observer.reconnectionAttempted()

                    currentServerIndex = (currentServerIndex + 1) % endpoints.size
                    uri = endpoints[currentServerIndex]

                    reconnect()

                    delay(5000)
                } while (!isOpen)

                log.info("Connected to $uri")
            }
        }
    }

    override fun onMessage(message: String?) {
        observer.messageReceived()

        if (message == null) {
            log.error("Empty message received")
            observer.messageDiscarded(DiscardReason.EMPTY_MESSAGE)

            return
        }

        val lightningStrike = try {
            objectMapper.readValue<BlitzortungLightningStrike>(lzwDecompress(message))
        } catch (e: Exception) {
            log.error("Deserialization failed", e)
            observer.messageDiscarded(DiscardReason.INVALID_MESSAGE)

            return
        }

        if (
            lightningStrike.lat > topLeft.lat || lightningStrike.lat < bottomRight.lat ||
            lightningStrike.lon < topLeft.lon || lightningStrike.lon > bottomRight.lon
        ) {
            observer.messageDiscarded(DiscardReason.OUTSIDE_MONITORED_AREA)
            return
        }

        onLightningStrike(lightningStrike.toLightningStrike())
        observer.lightningStrikeStored()
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        observer.connectionChanged(true)
        send("{\"a\":111}")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        observer.connectionChanged(false)
        log.info("Closed web socket: $reason")
    }

    override fun onError(ex: Exception?) {
        observer.websocketError()
        log.error("Web socket error: $ex")
    }
}
