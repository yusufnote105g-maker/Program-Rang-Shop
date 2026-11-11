package com.example.habittracker.domain

import com.example.habittracker.domain.reminder.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ReminderTimeTest {

    private val zone = ZoneId.of("Europe/Moscow")

    @Test
    fun nextTrigger_usesTodayWhenTimeIsStillAhead() {
        val now = Instant.parse("2026-11-10T06:00:00Z") // 09:00 MSK
        val trigger = ReminderTime.nextTrigger(
            nowMillis = now.toEpochMilli(),
            hour = 10,
            minute = 30,
            zone = zone,
        )
        val local = Instant.ofEpochMilli(trigger).atZone(zone)
        assertEquals(10, local.hour)
        assertEquals(30, local.minute)
        assertEquals(now.atZone(zone).toLocalDate(), local.toLocalDate())
    }

    @Test
    fun nextTrigger_movesToTomorrowWhenTimeHasPassed() {
        val now = Instant.parse("2026-11-10T08:00:00Z") // 11:00 MSK
        val trigger = ReminderTime.nextTrigger(
            nowMillis = now.toEpochMilli(),
            hour = 10,
            minute = 0,
            zone = zone,
        )
        val local = Instant.ofEpochMilli(trigger).atZone(zone)
        assertEquals(now.atZone(zone).toLocalDate().plusDays(1), local.toLocalDate())
        assertEquals(10, local.hour)
    }

    @Test
    fun nextAfter_keepsLocalTimeAcrossDst() {
        val zoneWithDst = ZoneId.of("Europe/Berlin")
        // 25 октября 2026 — переход на зимнее время в Европе.
        val previous = Instant.parse("2026-10-24T06:00:00Z") // 08:00 CEST
        val next = ReminderTime.nextAfter(
            previousMillis = previous.toEpochMilli(),
            nowMillis = Instant.parse("2026-10-24T07:00:00Z").toEpochMilli(),
            zone = zoneWithDst,
        )
        val local = Instant.ofEpochMilli(next).atZone(zoneWithDst)
        assertEquals(8, local.hour)
        assertEquals(0, local.minute)
        assertTrue(next > previous.toEpochMilli())
    }

    @Test
    fun hourAndMinute_defaultsAreNine() {
        assertEquals(9, ReminderTime.hourOf(null, zone))
        assertEquals(0, ReminderTime.minuteOf(null, zone))
    }

    @Test
    fun format_alwaysUsesTwoDigits() {
        assertEquals("09:05", ReminderTime.format(9, 5))
        assertEquals("23:00", ReminderTime.format(23, 0))
    }
}
