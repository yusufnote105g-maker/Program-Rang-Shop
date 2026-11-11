package com.example.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.habittracker.MainActivity
import com.example.habittracker.R
import com.example.habittracker.di.BackgroundEntryPoint
import com.example.habittracker.domain.model.WidgetSnapshot
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Домашний виджет: уровень, ресурсы и сводка задач на сегодня. */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refreshAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, TodayWidgetProvider::class.java),
            )
            refreshAsync(context, manager, ids)
        } else {
            super.onReceive(context, intent)
        }
    }

    private fun refreshAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    BackgroundEntryPoint::class.java,
                )
                val repository = entry.taskRepository()
                repository.ensureUser()
                val snapshot = repository.widgetSnapshot()
                ids.forEach { id -> update(context, manager, id, snapshot) }
            } finally {
                pending.finish()
            }
        }
    }

    private fun update(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        snapshot: WidgetSnapshot,
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_today)
        views.setTextViewText(
            R.id.widget_stats,
            context.getString(
                R.string.widget_stats,
                snapshot.level,
                snapshot.xp,
                snapshot.xpToNext,
                snapshot.gold,
            ),
        )
        views.setTextViewText(
            R.id.widget_today,
            context.getString(
                R.string.widget_today,
                snapshot.habits,
                snapshot.dailiesDone,
                snapshot.dailiesDue,
                snapshot.todosOpen,
            ),
        )
        views.setTextViewText(
            R.id.widget_streak,
            snapshot.bestStreakText?.let { text ->
                context.getString(R.string.widget_streak, snapshot.bestStreak, text)
            } ?: context.getString(R.string.widget_streak_empty),
        )

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val refresh = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, TodayWidgetProvider::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)
        views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
        manager.updateAppWidget(widgetId, views)
    }

    companion object {
        const val ACTION_REFRESH = "com.example.habittracker.ACTION_REFRESH_WIDGET"
    }
}
