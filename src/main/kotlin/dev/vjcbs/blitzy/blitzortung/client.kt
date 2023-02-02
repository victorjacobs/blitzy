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

class BlitzortungClient(
    private val topLeft: Coordinate,
    private val bottomRight: Coordinate,
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
        val lightningStrike = message?.let {
            try {
                objectMapper.readValue<BlitzortungLightningStrike>(lzwDecompress(it))
            } catch (e: Exception) {
                log.error("Deserialization failed", e)
                null
            }
        } ?: run {
            log.error("Empty message received")
            null
        }

        lightningStrike?.let {
            if (it.lat > topLeft.lat || it.lat < bottomRight.lat || it.lon < topLeft.lon || it.lon > bottomRight.lon) {
                return
            }

            onLightningStrike(it.toLightningStrike())
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        send("{\"a\":418}")
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) = log.info("Closed web socket: $reason")

    override fun onError(ex: Exception?) = log.error("Web socket error: $ex")
}
