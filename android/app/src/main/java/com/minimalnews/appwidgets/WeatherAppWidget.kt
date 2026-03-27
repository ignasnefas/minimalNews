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
import com.minimalnews.data.models.WeatherData

class WeatherAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> displayFromCache(context, manager, id) }
        scheduleUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_WEATHER") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherAppWidget::class.java))
            ids.forEach { id -> displayFromCache(context, manager, id) }
            scheduleUpdate(context)
        }
    }

    private fun scheduleUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<WidgetDataWorker>()
            .setInputData(workDataOf("widget_type" to "weather"))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("weather_widget_now", ExistingWorkPolicy.REPLACE, work)
    }

    companion object {
        fun displayFromCache(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_weather)
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val json = prefs.getString("weather_data", null)

            if (json != null) {
                try {
                    val w = Gson().fromJson(json, WeatherData::class.java)
                    views.setTextViewText(R.id.weather_location_text, w.location)
                    views.setTextViewText(R.id.weather_temp, "${w.current.temp.toInt()}°C")
                    views.setTextViewText(R.id.weather_condition, "${w.current.icon} ${w.current.condition}")
                    views.setTextViewText(
                        R.id.weather_details,
                        "Humidity ${w.current.humidity}% · Wind ${w.current.windSpeed.toInt()} km/h"
                    )
                } catch (_: Exception) {
                    views.setTextViewText(R.id.weather_temp, "--°C")
                }
            } else {
                views.setTextViewText(R.id.weather_temp, "...")
                views.setTextViewText(R.id.weather_condition, "Tap to load")
            }

            // Refresh
            val refreshIntent = Intent(context, WeatherAppWidget::class.java).apply {
                action = "com.minidash.REFRESH_WEATHER"
            }
            views.setOnClickPendingIntent(
                R.id.refresh_button,
                PendingIntent.getBroadcast(
                    context, 1, refreshIntent,
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
