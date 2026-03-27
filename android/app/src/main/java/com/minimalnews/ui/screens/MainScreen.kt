package com.minimalnews.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.minimalnews.ui.components.TerminalDivider
import com.minimalnews.ui.theme.terminalThemes
import com.minimalnews.ui.viewmodel.MainViewModel
import com.minimalnews.ui.widgets.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val enabled = viewModel.enabledWidgets.value
    val showManager = viewModel.widgetManagerOpen.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "minidash",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    // Theme toggle
                    Text(
                        text = "[theme]",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { viewModel.cycleTheme() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    // Widget manager
                    Text(
                        text = "⚙",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            viewModel.widgetManagerOpen.value = !showManager
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "minimal news v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${enabled.size} widgets active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(enabled, key = { it }) { widgetId ->
                    RenderWidget(widgetId, viewModel)
                }
            }

            // Widget Manager overlay
            if (showManager) {
                WidgetManagerSheet(viewModel)
            }
        }
    }
}

@Composable
private fun RenderWidget(id: String, viewModel: MainViewModel) {
    when (id) {
        "clock" -> ClockWidgetComposable()
        "quote" -> QuoteWidgetComposable(viewModel.repository)
        "weather" -> WeatherWidgetComposable(viewModel.repository)
        "news" -> NewsWidgetComposable(viewModel.repository)
        "hackernews" -> HackerNewsWidgetComposable(viewModel.repository)
        "reddit" -> RedditWidgetComposable(viewModel.repository)
        "crypto" -> CryptoWidgetComposable(viewModel.repository)
        "worldclocks" -> WorldClocksWidgetComposable()
        "todo" -> TodoWidgetComposable(viewModel.database)
        "trending" -> TrendingWidgetComposable(viewModel.repository)
        "systeminfo" -> SystemInfoWidgetComposable()
    }
}

@Composable
private fun WidgetManagerSheet(viewModel: MainViewModel) {
    val enabled = viewModel.enabledWidgets.value
    val disabled = viewModel.allWidgetIds.filter { it !in enabled }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Widget Manager",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "[close]",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { viewModel.widgetManagerOpen.value = false }
            )
        }
        Spacer(Modifier.height(12.dp))

        // Theme selection
        Text(
            "Theme",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            terminalThemes.forEach { theme ->
                val isSelected = theme.name == viewModel.currentTheme.value
                Text(
                    text = if (isSelected) "[${theme.displayName}]" else " ${theme.displayName} ",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { viewModel.setTheme(theme.name) }
                        .then(
                            if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary)
                            else Modifier
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bulk actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "[enable all]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { viewModel.enableAll() }
            )
            Text(
                "[disable all]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable { viewModel.disableAll() }
            )
            Text(
                "[reset]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { viewModel.resetWidgets() }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Enabled widgets
        Text(
            "Enabled",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        enabled.forEachIndexed { idx, id ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    viewModel.widgetDisplayName(id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (idx > 0) {
                        Text(
                            "↑",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { viewModel.moveWidget(id, "up") }
                        )
                    }
                    if (idx < enabled.lastIndex) {
                        Text(
                            "↓",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { viewModel.moveWidget(id, "down") }
                        )
                    }
                    Text(
                        "✕",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable { viewModel.toggleWidget(id) }
                    )
                }
            }
            TerminalDivider()
        }

        if (disabled.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Available",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            disabled.forEach { id ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleWidget(id) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        viewModel.widgetDisplayName(id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "+",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                TerminalDivider()
            }
        }
    }
}
