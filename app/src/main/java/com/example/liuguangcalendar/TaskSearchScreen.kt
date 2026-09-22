package com.example.liuguangcalendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.time.format.DateTimeFormatter

internal fun searchTasks(tasks: List<CalendarTask>, query: String): List<CalendarTask> {
    val keywords = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return tasks.filter { task ->
        keywords.all { word ->
            task.title.contains(word, ignoreCase = true) || task.category.contains(word, ignoreCase = true)
        }
    }.sortedWith(compareBy<CalendarTask> { it.completed }.thenBy { it.date }.thenBy { it.time })
}

@Composable
internal fun TaskSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    tasks: List<CalendarTask>,
    onBack: () -> Unit,
    onOpen: (CalendarTask) -> Unit,
    onToggle: (Long) -> Unit,
    onEdit: (CalendarTask) -> Unit,
    onDelete: (Long) -> Unit
) {
    val results = searchTasks(tasks, query)
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().imePadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBackIosNew, contentDescription = "返回日历")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("搜索待办") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "清空搜索")
                        }
                    },
                    modifier = Modifier.weight(1f).focusRequester(focus).testTag("task-search")
                )
            }
            Text(
                text = if (query.isBlank()) "全部待办 · ${results.size}" else "搜索结果 · ${results.size}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            if (results.isEmpty()) {
                Text("未找到相关待办", modifier = Modifier.padding(vertical = 24.dp))
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(results, key = { it.id }) { task ->
                    Column(Modifier.testTag("search-result-${task.id}")) {
                        Text(
                            task.date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                        TaskRow(
                            task = task,
                            onToggle = { onToggle(task.id) },
                            onEdit = { onEdit(task) },
                            onDelete = { onDelete(task.id) },
                            onOpen = { keyboard?.hide(); onOpen(task) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    }
                }
            }
        }
    }
}
