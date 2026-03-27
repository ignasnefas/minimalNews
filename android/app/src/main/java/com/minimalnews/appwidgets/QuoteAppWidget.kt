package com.minimalnews.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import com.google.gson.Gson
import com.minimalnews.MainActivity
import com.minimalnews.R
import com.minimalnews.data.models.Quote

class QuoteAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> displayFromCache(context, manager, id) }
        scheduleUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_QUOTE") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuoteAppWidget::class.java))
            ids.forEach { id -> displayFromCache(context, manager, id) }
            scheduleUpdate(context)
        }
    }

    private fun scheduleUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<WidgetDataWorker>()
            .setInputData(workDataOf("widget_type" to "quote"))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("quote_widget_now", ExistingWorkPolicy.REPLACE, work)
    }

    companion object {
        fun displayFromCache(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_quote)
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val json = prefs.getString("quote_data", null)

            if (json != null) {
                try {
                    val quote = Gson().fromJson(json, Quote::class.java)
                    views.setTextViewText(R.id.quote_text, "\"${quote.text}\"")
                    views.setTextViewText(R.id.quote_author, "— ${quote.author ?: "Unknown"}")
                } catch (_: Exception) {
                    views.setTextViewText(R.id.quote_text, "Tap to load")
                }
            } else {
                views.setTextViewText(R.id.quote_text, "Tap ↻ for a quote")
                views.setTextViewText(R.id.quote_author, "")
            }

            // Refresh
            val refreshIntent = Intent(context, QuoteAppWidget::class.java).apply {
                action = "com.minidash.REFRESH_QUOTE"
            }
            views.setOnClickPendingIntent(
                R.id.refresh_button,
                PendingIntent.getBroadcast(
                    context, 4, refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            // Open app
            val openIntent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(
                R.id.widget_container,
                PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)
            )

            manager.updateAppWidget(id, views)
        }
    }
}
