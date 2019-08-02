package dev.vjcbs.blitzy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.delay
import org.java_websocket.client.WebSocketClient
import org.java_websocket.enums.ReadyState
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class BlitzOrtungClient(
    private val topLeft: Coordinate,
    private val bottomRight: Coordinate,
    private val onLightningStrike: (LightningStrike) -> Unit
) : WebSocketClient(
    URI("ws://ws1.blitzortung.org:8056/")
) {

    private val log = logger()

    private val objectMapper = ObjectMapper().apply {
        registerKotlinModule()
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    suspend fun startAndKeepAlive() {
        connect()

        while (readyState == ReadyState.NOT_YET_CONNECTED) {
            delay(100)
        }

        while (true) {
            if (isClosed) {
                log.info("Websocket closed")

                reconnect()

                while (!isOpen) {
                    delay(100)
                }

                log.info("Websocket reconnected")
            }

            delay(1000)
        }
    }

    override fun onMessage(message: String?) {
        val lightningStrike = message?.let {
            try {
                objectMapper.readValue<BlitzOrtungLightningStrike>(it)
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
        log.info("Opened websocket")

        send(
            "{\"west\":${topLeft.lon},\"east\":${bottomRight.lon}," +
                "\"north\":${topLeft.lat},\"south\":${bottomRight.lat}}"
        )
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) = log.info("Closed websocket")

    override fun onError(ex: Exception?) = log.error("Websocket error: $ex")

    private data class BlitzOrtungLightningStrike(
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
