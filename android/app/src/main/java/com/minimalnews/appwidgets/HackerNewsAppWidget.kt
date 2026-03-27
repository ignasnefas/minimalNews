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
import com.google.gson.reflect.TypeToken
import com.minimalnews.MainActivity
import com.minimalnews.R
import com.minimalnews.data.models.HackerNewsItem

class HackerNewsAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> displayFromCache(context, manager, id) }
        scheduleUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_HACKERNEWS") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, HackerNewsAppWidget::class.java))
            ids.forEach { id -> displayFromCache(context, manager, id) }
            scheduleUpdate(context)
        }
    }

    private fun scheduleUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<WidgetDataWorker>()
            .setInputData(workDataOf("widget_type" to "hackernews"))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("hn_widget_now", ExistingWorkPolicy.REPLACE, work)
    }

    companion object {
        fun displayFromCache(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_news)
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val json = prefs.getString("hn_data", null)

            views.setTextViewText(R.id.widget_title, "hackernews")

            if (json != null) {
                try {
                    val type = object : TypeToken<List<HackerNewsItem>>() {}.type
                    val stories: List<HackerNewsItem> = Gson().fromJson(json, type)
                    val display = stories.take(5).mapIndexed { i, s ->
                        "${i + 1}. ${s.title}\n   ▲${s.score} · ${s.by}"
                    }.joinToString("\n\n")
                    views.setTextViewText(R.id.news_content, display)
                    views.setTextViewText(R.id.news_count, "${stories.size} stories")
                } catch (_: Exception) {
                    views.setTextViewText(R.id.news_content, "Error loading")
                }
            } else {
                views.setTextViewText(R.id.news_content, "Tap ↻ to load")
            }

            // Refresh
            val refreshIntent = Intent(context, HackerNewsAppWidget::class.java).apply {
                action = "com.minidash.REFRESH_HACKERNEWS"
            }
            views.setOnClickPendingIntent(
                R.id.refresh_button,
                PendingIntent.getBroadcast(
                    context, 6, refreshIntent,
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
