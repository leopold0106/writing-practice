package com.example.writingpractice.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.writingpractice.R
import com.example.writingpractice.data.local.db.dao.ProgressDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class StreakWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun progressDao(): ProgressDao
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val progressDao = entryPoint.progressDao()

        CoroutineScope(Dispatchers.Main).launch {
            val dates = progressDao.observeAllActiveDates().first()
            val streak = computeStreak(dates)
            appWidgetIds.forEach { widgetId ->
                updateWidgetViews(context, appWidgetManager, widgetId, streak)
            }
        }
    }

    private fun updateWidgetViews(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        streak: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_streak)
        views.setTextViewText(R.id.widget_streak_count, streak.toString())
        if (streak == 0) {
            views.setTextViewText(R.id.widget_fire_icon, "💤")
            views.setTextViewText(R.id.widget_streak_label, "일 연속")
        } else {
            views.setTextViewText(R.id.widget_fire_icon, "🔥")
            views.setTextViewText(R.id.widget_streak_label, "일 연속")
        }
        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private fun computeStreak(dates: List<String>): Int {
        if (dates.isEmpty()) return 0
        val today = LocalDate.now()
        val sorted = dates.map { LocalDate.parse(it) }.sortedDescending()
        val mostRecent = sorted.first()
        if (mostRecent.isBefore(today.minusDays(1))) return 0
        var streak = 0
        var expected = if (mostRecent == today) today else today.minusDays(1)
        for (date in sorted) {
            if (date == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (date.isBefore(expected)) {
                break
            }
        }
        return streak
    }

    companion object {
        const val ACTION_STREAK_UPDATED = "com.example.writingpractice.ACTION_STREAK_UPDATED"
    }
}
