package com.example.habittracker.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.habittracker.di.BackgroundEntryPoint

/**
 * После перезагрузки AlarmManager очищается. Восстанавливаем все включённые
 * напоминания из Room; просроченные сработают сразу и перепланируются.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BackgroundEntryPoint::class.java,
                )
                val repository = entry.taskRepository()
                val scheduler = entry.reminderScheduler()
                repository.ensureUser()
                repository.getReminderTasks().forEach { task ->
                    val trigger = task.reminderAt ?: return@forEach
                    scheduler.schedule(task.id, trigger, task.text)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
