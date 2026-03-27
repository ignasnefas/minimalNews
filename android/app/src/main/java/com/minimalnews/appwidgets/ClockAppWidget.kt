package com.minimalnews.appwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.minimalnews.MainActivity
import com.minimalnews.R
import android.app.PendingIntent
import java.text.SimpleDateFormat
import java.util.*

class ClockAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_CLOCK") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockAppWidget::class.java))
            ids.forEach { id -> updateWidget(context, manager, id) }
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            val now = Date()

            views.setTextViewText(R.id.clock_time, SimpleDateFormat("HH:mm", Locale.getDefault()).format(now))
            views.setTextViewText(R.id.clock_seconds, SimpleDateFormat(":ss", Locale.getDefault()).format(now))
            views.setTextViewText(R.id.clock_date, SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now))

            // Open app on click
            val openIntent = Intent(context, MainActivity::class.java)
            val openPending = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, openPending)

            manager.updateAppWidget(id, views)
        }
    }
}
