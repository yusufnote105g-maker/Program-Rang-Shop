package com.example.habittracker.widget

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** После любого изменения данных просим виджет перечитать Room. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun refresh() {
        runCatching {
            context.sendBroadcast(
                Intent(context, TodayWidgetProvider::class.java)
                    .setAction(TodayWidgetProvider.ACTION_REFRESH),
            )
        }
    }
}
