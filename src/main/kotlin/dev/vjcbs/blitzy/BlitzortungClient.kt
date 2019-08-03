package dev.vjcbs.blitzy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.delay
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class BlitzortungClient(
    private val topLeft: Coordinate,
    private val bottomRight: Coordinate,
    private val onLightningStrike: (LightningStrike) -> Unit
) : WebSocketClient(
    URI("ws://ws1.blitzortung.org:8056/")
) {

    private val log = logger()

    private var currentServer = 1

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

                    uri = nextServerUri()
                    reconnect()

                    delay(5000)
                } while (!isOpen)

                log.info("Connected to ${serverUri()}")
            }
        }
    }

    private fun serverUri() = URI("ws://ws$currentServer.blitzortung.org:8056/")

    private fun nextServerUri(): URI {
        currentServer = (currentServer % 5) + 1
        return serverUri()
    }

    override fun onMessage(message: String?) {
        val lightningStrike = message?.let {
            try {
                objectMapper.readValue<BlitzortungLightningStrike>(it)
            } catch (e: Exception) {
                log.error("Deserialization failed", e)
                null
            }
        } ?: run {
            log.error("Empty message received")
            null
        }

        lightningStrike?.let {
            onLightningStrike(it.toLightningStrike())
        }
    }

    override fun onOpen(handshakedata: ServerHandshake?) {
        send(
            "{\"west\":${topLeft.lon},\"east\":${bottomRight.lon}," +
                "\"north\":${topLeft.lat},\"south\":${bottomRight.lat}}"
        )
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) = log.info("Closed web socket: $reason")

    override fun onError(ex: Exception?) = log.error("Web socket error: $ex")

    private data class BlitzortungLightningStrike(
        val time: Long,
        val lat: Double,
        val lon: Double
    ) {
        fun toLightningStrike() = LightningStrike(
            timestampNanos = time,
            coordinate = Coordinate(lat, lon)
        )
    }
}
