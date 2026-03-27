package com.minimalnews.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimalnews.data.local.AppDatabase
import com.minimalnews.data.models.TodoItem
import com.minimalnews.ui.components.TerminalBox
import kotlinx.coroutines.launch

@Composable
fun TodoWidgetComposable(database: AppDatabase) {
    val todos by database.todoDao().getAll().collectAsState(initial = emptyList())
    var newTodoText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    TerminalBox(title = "todo", status = "${todos.count { it.completed }}/${todos.size} done") {
        Column(Modifier.padding(12.dp)) {
            // Input
            TextField(
                value = newTodoText,
                onValueChange = { newTodoText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("new task", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), fontSize = 13.sp) },
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val t = newTodoText.trim()
                    if (t.isNotEmpty()) {
                        scope.launch { database.todoDao().insert(TodoItem(text = t)) }
                        newTodoText = ""
                    }
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(Modifier.height(8.dp))

            // Items
            todos.forEach { todo ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch {
                                database.todoDao().update(todo.copy(completed = !todo.completed))
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (todo.completed) "[x] " else "[ ] ",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = todo.text.ifEmpty { "(empty)" },
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = " [del]",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            scope.launch { database.todoDao().delete(todo) }
                        }
                    )
                }
            }
        }
    }
}


