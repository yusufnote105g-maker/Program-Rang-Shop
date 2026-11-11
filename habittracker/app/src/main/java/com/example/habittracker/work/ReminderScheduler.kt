package com.example.habittracker.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Планировщик напоминаний. Точный alarm — best effort: без разрешения
 * SCHEDULE_EXACT_ALARM система может сдвинуть срабатывание, поэтому
 * используем setAndAllowWhileIdle как безопасный fallback.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun schedule(taskId: String, triggerAt: Long, taskText: String) {
        val manager = alarmManager ?: return
        runCatching {
            val pi = pendingIntent(taskId, taskText, PendingIntent.FLAG_UPDATE_CURRENT)
                ?: return@runCatching
            val exactAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                manager.canScheduleExactAlarms()
            try {
                if (exactAllowed) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (_: SecurityException) {
                // Разрешение могли отозвать между проверкой и вызовом.
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    fun cancel(taskId: String) {
        val manager = alarmManager ?: return
        runCatching {
            val pi = pendingIntent(taskId, null, PendingIntent.FLAG_NO_CREATE)
                ?: return@runCatching
            manager.cancel(pi)
            pi.cancel()
        }
    }

    private fun pendingIntent(taskId: String, taskText: String?, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            data = Uri.parse("habittracker://reminder/$taskId")
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            if (taskText != null) putExtra(ReminderReceiver.EXTRA_TASK_TEXT, taskText)
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
