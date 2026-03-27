package com.minimalnews.ui.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.minimalnews.ui.components.TerminalBox
import com.minimalnews.ui.components.TerminalDivider
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorldClocksWidgetComposable() {
    var zones by remember {
        mutableStateOf(
            listOf("UTC", "America/New_York", "Europe/London", "Asia/Tokyo")
        )
    }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var newZone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    TerminalBox(title = "world-clocks", status = "${zones.size} zones") {
        zones.forEach { zoneId ->
            val tz = TimeZone.getTimeZone(zoneId)
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.timeZone = tz
            val dateFmt = SimpleDateFormat("MMM dd", Locale.getDefault())
            dateFmt.timeZone = tz

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = zoneId.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = dateFmt.format(Date(currentTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = sdf.format(Date(currentTime)),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (zones.size > 1) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clickable {
                            zones = zones.filter { it != zoneId }
                        }
                    )
                }
            }
            TerminalDivider()
        }

        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$ add: ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            OutlinedTextField(
                value = newZone,
                onValueChange = { newZone = it },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = true,
                placeholder = {
                    Text(
                        "London, Tokyo, Europe/Paris...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier
                    .weight(1f),

                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val input = newZone.trim()
                    if (input.isNotBlank()) {
                        val resolved = resolveTimezoneId(input)
                        if (resolved != null && resolved !in zones) {
                            zones = zones + resolved
                            newZone = ""
                        } else if (resolved == null) {
                            // Show hint by clearing and leaving — user sees placeholder again
                            newZone = ""
                        }
                    }
                }),
                visualTransformation = VisualTransformation.None,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    cursorColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    }
}

/**
 * Resolve a user-typed string to a valid IANA timezone ID.
 * Accepts exact IDs (Europe/Paris), case-insensitive IDs, city-only names (paris → Europe/Paris),
 * or common abbreviations (UTC, EST, PST, etc.).
 */
private fun resolveTimezoneId(input: String): String? {
    val allIds = TimeZone.getAvailableIDs().toList()
    // 1. Exact match
    if (allIds.any { it.equals(input, ignoreCase = false) }) return input
    // 2. Case-insensitive exact match
    allIds.firstOrNull { it.equals(input, ignoreCase = true) }?.let { return it }
    // 3. City suffix match  e.g. "paris" → "Europe/Paris"
    val lowered = input.lowercase()
    allIds.firstOrNull { it.substringAfterLast('/').lowercase() == lowered }?.let { return it }
    // 4. Partial city name match  e.g. "new_york" or "new york"
    val normalized = lowered.replace(' ', '_')
    allIds.firstOrNull { it.substringAfterLast('/').lowercase() == normalized }?.let { return it }
    // 5. Contains match
    allIds.firstOrNull { it.lowercase().contains(lowered) }?.let { return it }
    // 6. Known abbreviations
    val abbrev = mapOf(
        "est" to "America/New_York", "cst" to "America/Chicago",
        "mst" to "America/Denver", "pst" to "America/Los_Angeles",
        "gmt" to "GMT", "ist" to "Asia/Kolkata", "jst" to "Asia/Tokyo",
        "aest" to "Australia/Sydney", "cet" to "Europe/Paris",
        "bst" to "Europe/London"
    )
    abbrev[lowered]?.let { return it }
    // 7. Give up — not found
    return null
}
