package com.minimalnews.appwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.minimalnews.MainActivity
import com.minimalnews.R
import com.minimalnews.data.local.AppDatabase
import com.minimalnews.data.models.TodoItem
import kotlinx.coroutines.*

class TodoAppWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> displayWidget(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.minidash.REFRESH_TODO") {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodoAppWidget::class.java))
            ids.forEach { id -> displayWidget(context, manager, id) }
        }
    }

    companion object {
        fun displayWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo)

            // Fetch todos from database in a coroutine
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val todos = db.todoDao().getAllSync()
                    val completed = todos.count { it.completed }

                    val display = if (todos.isEmpty()) {
                        "No tasks yet"
                    } else {
                        todos.take(8).joinToString("\n") { todo ->
                            "${if (todo.completed) "[x]" else "[ ]"} ${todo.text}"
                        }
                    }

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.todo_content, display)
                        views.setTextViewText(R.id.todo_count, "$completed/${todos.size}")

                        // Open app
                        val openIntent = Intent(context, MainActivity::class.java)
                        views.setOnClickPendingIntent(
                            R.id.widget_container,
                            PendingIntent.getActivity(
                                context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE
                            )
                        )

                        // Refresh
                        val refreshIntent = Intent(context, TodoAppWidget::class.java).apply {
                            action = "com.minidash.REFRESH_TODO"
                        }
                        views.setOnClickPendingIntent(
                            R.id.refresh_button,
                            PendingIntent.getBroadcast(
                                context, 3, refreshIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )

                        manager.updateAppWidget(id, views)
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.todo_content, "Open app to add tasks")
                        manager.updateAppWidget(id, views)
                    }
                }
            }
        }
    }
}
