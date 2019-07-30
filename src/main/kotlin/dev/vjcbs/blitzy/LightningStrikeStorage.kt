package dev.vjcbs.blitzy

import java.time.Clock
import java.util.LinkedList
import java.util.concurrent.PriorityBlockingQueue

class LightningStrikeStorage {

    private val log = logger()

    private val lightningStrikeTtl = 10 * 60 * 1000

    private val clock = Clock.systemUTC()

    private val lightningStrikes = PriorityBlockingQueue<LightningStrike>(11) { a, b ->
        (a.timestampNanos - b.timestampNanos).compareTo(0)
    }

    fun add(lightningStrike: LightningStrike) = lightningStrikes.add(lightningStrike)

    fun get() = LinkedList(lightningStrikes)

    fun asArray() = lightningStrikes.map {
        doubleArrayOf(it.coordinate.lat, it.coordinate.lon)
    }.toTypedArray()

    fun size() = lightningStrikes.size

    fun timestampOldestLightningStrike() = lightningStrikes.peek()?.timestampNanos

    fun prune() {
        var removedEntries = 0
        val timeThresholdInNanos = (clock.millis() - lightningStrikeTtl) * 1_000_000
        var headOfQueue = lightningStrikes.peek()

        while (headOfQueue?.timestampNanos ?: Long.MAX_VALUE < timeThresholdInNanos) {
            headOfQueue = lightningStrikes.poll()
            removedEntries++
        }

        log.info("Removed $removedEntries entries from storage, currently storing ${size()} entries")
    }
}
