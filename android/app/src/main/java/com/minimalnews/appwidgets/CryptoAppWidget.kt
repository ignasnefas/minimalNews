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
import com.minimalnews.data.models.CryptoPrice

class CryptoAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> displayFromCache(context, manager, id) }
        scheduleUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_CRYPTO") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CryptoAppWidget::class.java))
            ids.forEach { id -> displayFromCache(context, manager, id) }
            scheduleUpdate(context)
        }
    }

    private fun scheduleUpdate(context: Context) {
        val work = OneTimeWorkRequestBuilder<WidgetDataWorker>()
            .setInputData(workDataOf("widget_type" to "crypto"))
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("crypto_widget_now", ExistingWorkPolicy.REPLACE, work)
    }

    companion object {
        fun displayFromCache(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_crypto)
            val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
            val json = prefs.getString("crypto_data", null)

            if (json != null) {
                try {
                    val type = object : TypeToken<List<CryptoPrice>>() {}.type
                    val prices: List<CryptoPrice> = Gson().fromJson(json, type)
                    val display = prices.joinToString("\n") { p ->
                        val arrow = if (p.change24h >= 0) "▲" else "▼"
                        val change = "%.1f%%".format(p.change24h)
                        "${p.symbol.uppercase()}  $${formatPrice(p.price)}  $arrow$change"
                    }
                    views.setTextViewText(R.id.crypto_content, display)
                } catch (_: Exception) {
                    views.setTextViewText(R.id.crypto_content, "Error loading")
                }
            } else {
                views.setTextViewText(R.id.crypto_content, "Tap ↻ to load")
            }

            // Refresh
            val refreshIntent = Intent(context, CryptoAppWidget::class.java).apply {
                action = "com.minidash.REFRESH_CRYPTO"
            }
            views.setOnClickPendingIntent(
                R.id.refresh_button,
                PendingIntent.getBroadcast(
                    context, 5, refreshIntent,
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

        private fun formatPrice(price: Double): String = when {
            price >= 1000 -> "%,.0f".format(price)
            price >= 1 -> "%.2f".format(price)
            else -> "%.4f".format(price)
        }
    }
}
