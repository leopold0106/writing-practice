package com.example.writingpractice.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Color
import android.widget.RemoteViews
import com.example.writingpractice.R
import com.example.writingpractice.data.local.db.dao.ProgressDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dates = progressDao.observeAllActiveDates().first()
                val streak = computeStreak(dates)
                appWidgetIds.forEach { widgetId ->
                    updateWidgetViews(context, appWidgetManager, widgetId, streak)
                }
            } finally {
                pending.finish()
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

        val (bgRes, countColor, labelColor, appNameColor) = when {
            streak >= 100 -> Tier(
                R.drawable.widget_bg_red,
                Color.WHITE,
                Color.parseColor("#FFCDD2"),
                Color.parseColor("#EF9A9A")
            )
            streak >= 10 -> Tier(
                R.drawable.widget_bg_orange,
                Color.WHITE,
                Color.parseColor("#FFE0B2"),
                Color.parseColor("#FFCC80")
            )
            else -> Tier(
                R.drawable.widget_bg_yellow,
                Color.parseColor("#F57F17"),
                Color.parseColor("#F57F17"),
                Color.parseColor("#FFB300")
            )
        }

        views.setInt(R.id.widget_root, "setBackgroundResource", bgRes)
        views.setTextViewText(R.id.widget_streak_count, streak.toString())
        views.setTextColor(R.id.widget_streak_count, countColor)
        views.setTextColor(R.id.widget_streak_label, labelColor)
        views.setTextColor(R.id.widget_app_name, appNameColor)
        views.setTextViewText(R.id.widget_fire_icon, if (streak == 0) "💤" else "🔥")

        appWidgetManager.updateAppWidget(widgetId, views)
    }

    private data class Tier(
        val bgRes: Int,
        val countColor: Int,
        val labelColor: Int,
        val appNameColor: Int
    )

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
