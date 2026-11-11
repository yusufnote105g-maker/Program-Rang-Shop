package com.example.habittracker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.habittracker.di.BackgroundEntryPoint

/** Один выстрел будильника: показать уведомление и запланировать следующий. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BackgroundEntryPoint::class.java,
                )
                val firing = entry.taskRepository().onReminderFired(taskId) ?: return@launch
                firing.nextTriggerAt?.let { next ->
                    entry.reminderScheduler().schedule(firing.taskId, next, firing.text)
                }
                ReminderNotifications.show(context, firing)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.example.habittracker.ACTION_REMIND"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TEXT = "task_text"
    }
}
