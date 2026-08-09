package dev.vjcbs.blitzy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class LightningStrikeStorageTest {
    @Test
    fun `prunes expired strikes without removing fresh strikes`() {
        val now = Instant.parse("2026-08-09T12:00:00Z")
        val ttlMillis = 60_000
        val storage = LightningStrikeStorage(ttlMillis, Clock.fixed(now, ZoneOffset.UTC))

        storage.add(strikeAt(now.minusMillis(ttlMillis.toLong() + 1)))
        storage.add(strikeAt(now))

        assertEquals(1, storage.prune())
        assertEquals(1, storage.size())
    }

    private fun strikeAt(timestamp: Instant) = LightningStrike(
        timestampNanos = timestamp.epochSecond * 1_000_000_000 + timestamp.nano,
        coordinate = Coordinate(50.0, 4.0)
    )
}
