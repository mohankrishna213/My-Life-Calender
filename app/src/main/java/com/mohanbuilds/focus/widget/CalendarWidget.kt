package com.mohanbuilds.focus.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.mohanbuilds.focus.R
import com.mohanbuilds.focus.data.CalendarRepository
import com.mohanbuilds.focus.domain.daysRemaining
import java.time.LocalDate

class CalendarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    companion object {
        fun update(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val goal = CalendarRepository.loadGoalSync(context)
            val views = RemoteViews(context.packageName, R.layout.widget_calendar)
            if (goal == null) {
                views.setTextViewText(R.id.widget_title, "Focus")
                views.setTextViewText(R.id.widget_countdown, "Set a goal to begin")
            } else {
                views.setTextViewText(R.id.widget_title, goal.title)
                val endDate = runCatching { LocalDate.parse(goal.endDate) }.getOrNull()
                if (endDate != null) {
                    views.setTextViewText(R.id.widget_countdown, "${daysRemaining(LocalDate.now(), endDate)} days left")
                } else {
                    views.setTextViewText(R.id.widget_countdown, "Set a goal to begin")
                }
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
