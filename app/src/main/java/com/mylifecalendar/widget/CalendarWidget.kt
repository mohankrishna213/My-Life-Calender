package com.mylifecalendar.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.mylifecalendar.R
import com.mylifecalendar.data.Goal
import com.mylifecalendar.domain.daysRemaining
import java.time.LocalDate
import kotlinx.serialization.json.Json

class CalendarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val goal = context.getSharedPreferences("calendar", Context.MODE_PRIVATE)
                .getString("goal", null)
                ?.let { runCatching { Json.decodeFromString<Goal>(it) }.getOrNull() }
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            if (goal == null) {
                views.setTextViewText(R.id.widget_title, "My Life Calendar")
                views.setTextViewText(R.id.widget_countdown, "Set a goal to begin")
            } else {
                views.setTextViewText(R.id.widget_title, goal.title)
                views.setTextViewText(R.id.widget_countdown, "${daysRemaining(LocalDate.now(), LocalDate.parse(goal.endDate))} days left")
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
