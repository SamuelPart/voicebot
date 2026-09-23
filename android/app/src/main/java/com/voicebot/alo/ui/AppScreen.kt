package com.voicebot.alo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voicebot.alo.core.log.EventLog
import com.voicebot.alo.core.settings.AloSettings
import com.voicebot.alo.data.db.DroppedEntity
import com.voicebot.alo.data.db.MessageEntity
import com.voicebot.alo.ui.theme.AloColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    permissionGranted: Boolean,
    settings: AloSettings.Snapshot,
    entries: List<EventLog.Entry>,
    stats: EventLog.Stats,
    messages: List<MessageEntity>,
    reviewQueue: List<DroppedEntity>,
    discardedCount: Int,
    isSpeaking: Boolean,
    banner: String?,
    onOpenPermissionSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onSimulateMessage: () -> Unit,
    onSimulateNoise: () -> Unit,
    onReplay: (MessageEntity) -> Unit,
    onReplayAll: () -> Unit,
    onStopReading: () -> Unit,
    onMarkReviewed: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onConsumeBanner: () -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onToggleGroups: (Boolean) -> Unit,
    onToggleHeadphones: (Boolean) -> Unit,
    onTogglePauseCalls: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onQuietHoursChange: (Int?, Int?) -> Unit,
    debugBuild: Boolean = false,
    onToggleTestMode: (Boolean) -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("En vivo", "Historial", "Revisar (${reviewQueue.size})", "Ajustes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Aló", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (permissionGranted) {
                                "${stats.captured} capturados · ${stats.discarded} descartados"
                            } else {
                                "Sin acceso a notificaciones"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (isSpeaking) {
                        IconButton(onClick = onStopReading) {
                            Icon(Icons.Default.Close, contentDescription = "Detener lectura")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!permissionGranted) {
                PermissionCard(onOpenPermissionSettings)
            }

            banner?.let { text ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(AloColors.info.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .clickable { onConsumeBanner() }
                        .padding(12.dp),
                ) {
                    Text(text, fontSize = 13.sp)
                }
            }

            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(title, fontSize = 12.sp) },
                    )
                }
            }

            when (tab) {
                0 -> LiveTab(stats, discardedCount, entries, isSpeaking, onSimulateMessage, onSimulateNoise, onStopReading)
                1 -> HistoryTab(messages, onReplay, onReplayAll, onStopReading, onClearHistory)
                2 -> ReviewTab(reviewQueue, onMarkReviewed)
                else -> SettingsTab(
                    settings = settings,
                    onOpenBatterySettings = onOpenBatterySettings,
                    onToggleVoice = onToggleVoice,
                    onToggleGroups = onToggleGroups,
                    onToggleHeadphones = onToggleHeadphones,
                    onTogglePauseCalls = onTogglePauseCalls,
                    onToggleVibrate = onToggleVibrate,
                    onLanguageChange = onLanguageChange,
                    onRetentionChange = onRetentionChange,
                    onQuietHoursChange = onQuietHoursChange,
                    debugBuild = debugBuild,
                    onToggleTestMode = onToggleTestMode,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(onOpen: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = AloColors.review.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = AloColors.review)
                Spacer(Modifier.width(8.dp))
                Text("Falta un paso: dale acceso a las notificaciones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Aló lee SOLO los mensajes de chat de WhatsApp en tu teléfono. " +
                    "El contenido no se envía a ningún servidor y puedes revocar el permiso cuando quieras.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpen) { Text("Abrir ajustes de acceso") }
        }
    }
}

@Composable
private fun LiveTab(
    stats: EventLog.Stats,
    discardedCount: Int,
    entries: List<EventLog.Entry>,
    isSpeaking: Boolean,
    onSimulateMessage: () -> Unit,
    onSimulateNoise: () -> Unit,
    onStopReading: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard("Leídos", stats.captured.toString(), AloColors.read, Modifier.weight(1f))
            StatCard("Descartados", stats.discarded.toString(), AloColors.discarded, Modifier.weight(1f))
            StatCard("Revisar", stats.needsReview.toString(), AloColors.review, Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onSimulateMessage, modifier = Modifier.weight(1f)) { Text("Probar voz", fontSize = 12.sp) }
            OutlinedButton(onClick = onSimulateNoise, modifier = Modifier.weight(1f)) { Text("Probar ruido", fontSize = 12.sp) }
            if (isSpeaking) {
                OutlinedButton(onClick = onStopReading) { Text("Silencio", fontSize = 12.sp) }
            }
        }

        Text(
            "Registro del filtro (capa que decidió)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )

        if (entries.isEmpty()) {
            EmptyState(
                "Aún no hay eventos.\nCuando llegue un mensaje de WhatsApp verás aquí si se leyó o se descartó y por qué.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(entries, key = { it.id + it.timestamp }) { entry ->
                    LogRow(entry)
                }
            }
        }
        if (discardedCount > 0) {
            Text(
                "$discardedCount avisos descartados en total (respaldos, resúmenes, llamadas, códigos)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.14f))) {
        Column(Modifier.padding(12.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LogRow(entry: EventLog.Entry) {
    val color = when (entry.kind) {
        EventLog.Entry.Kind.READ -> AloColors.read
        EventLog.Entry.Kind.DISCARDED -> AloColors.discarded
        EventLog.Entry.Kind.REVIEW -> AloColors.review
        EventLog.Entry.Kind.INFO -> AloColors.info
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                )
                Spacer(Modifier.width(8.dp))
                Text(entry.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(formatTime(entry.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (entry.detail.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(entry.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryTab(
    messages: List<MessageEntity>,
    onReplay: (MessageEntity) -> Unit,
    onReplayAll: () -> Unit,
    onStopReading: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onReplayAll, modifier = Modifier.weight(1f)) { Text("Leer últimos 5", fontSize = 12.sp) }
            OutlinedButton(onClick = onStopReading) { Text("Silencio", fontSize = 12.sp) }
            IconButton(onClick = onClearHistory) { Icon(Icons.Default.Delete, contentDescription = "Borrar historial") }
        }

        if (messages.isEmpty()) {
            EmptyState("Sin mensajes guardados todavía.\nTodo lo que se capture se guarda SOLO en este teléfono.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id + it.timestamp }) { message ->
                    MessageRow(message, onReplay)
                }
            }
        }
    }
}

@Composable
private fun MessageRow(message: MessageEntity, onReplay: (MessageEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (message.isGroup) "${message.sender ?: "Alguien"} · ${message.chatTitle}" else message.chatTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(formatTime(message.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                IconButton(onClick = { onReplay(message) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Repetir en voz alta", tint = AloColors.read)
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(message.text, fontSize = 13.sp)
            if (message.source == "BACKFILL") {
                Text("mensaje previo (no se leyó en voz alta)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ReviewTab(queue: List<DroppedEntity>, onMarkReviewed: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = AloColors.review.copy(alpha = 0.12f))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AloColors.review)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Nada se pierde: lo que el filtro no puede clasificar con seguridad se guarda aquí sin leerse. " +
                        "Estos casos alimentan las reglas OTA de la Fase 1.",
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (queue.isEmpty()) {
            EmptyState("Sin casos pendientes. El filtro está clasificando todo con seguridad.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(queue, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = AloColors.review, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Capa ${item.layer} · ${formatTime(item.timestamp)}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onMarkReviewed(item.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Check, contentDescription = "Marcar revisado", tint = AloColors.read)
                                }
                            }
                            Text(item.reason, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            item.preview?.let { Text(it, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    settings: AloSettings.Snapshot,
    onOpenBatterySettings: () -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onToggleGroups: (Boolean) -> Unit,
    onToggleHeadphones: (Boolean) -> Unit,
    onTogglePauseCalls: (Boolean) -> Unit,
    onToggleVibrate: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onQuietHoursChange: (Int?, Int?) -> Unit,
    debugBuild: Boolean,
    onToggleTestMode: (Boolean) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        ToggleRow("Leer en voz alta", "Apaga la voz sin dejar de capturar mensajes", settings.voiceEnabled, onToggleVoice)
        ToggleRow("Leer mensajes de grupos", null, settings.readGroups, onToggleGroups)
        ToggleRow("Solo con audífonos o Bluetooth", "Ideal para el auto o el trabajo", settings.onlyWithHeadphones, onToggleHeadphones)
        ToggleRow("Silencio durante llamadas", null, settings.pauseDuringCalls, onTogglePauseCalls)
        ToggleRow("Vibrar en vez de hablar", "Para reuniones", settings.vibrateInsteadOfSpeak, onToggleVibrate)

        if (debugBuild) {
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = AloColors.read.copy(alpha = 0.10f))) {
                Column(Modifier.padding(12.dp)) {
                    ToggleRow(
                        title = "Modo prueba (solo debug)",
                        subtitle = "Acepta notificaciones de adb (com.android.shell) para probar el filtro sin WhatsApp",
                        checked = settings.testMode,
                        onChange = onToggleTestMode,
                    )
                    Text(
                        "Con esto activo puedes ejecutar: adb shell cmd notification post -S messaging " +
                            "--conversation \"Mamá\" --message \"Mamá:hola\" prueba1 \"hola\"",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("Idioma de la voz")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("es-PE" to "Español PE", "es-MX" to "Español MX", "en-US" to "English").forEach { (tag, label) ->
                ChoiceChip(label, settings.languageTag == tag) { onLanguageChange(tag) }
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("No molestar (lectura en silencio)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("Desactivado", settings.quietHoursStart == null) { onQuietHoursChange(null, null) }
            ChoiceChip("22:00 – 07:00", settings.quietHoursStart == 22) { onQuietHoursChange(22, 7) }
            ChoiceChip("13:00 – 15:00", settings.quietHoursStart == 13) { onQuietHoursChange(13, 15) }
        }

        Spacer(Modifier.height(14.dp))
        SectionTitle("Guardar historial")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { days ->
                ChoiceChip("$days días", settings.retentionDays == days) { onRetentionChange(days) }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Teléfonos que matan el servicio en segundo plano", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Si en tu Xiaomi, Samsung, Huawei u Oppo la lectura se detiene, desactiva la optimización " +
                        "de batería para Aló y marca la app como \"sin restricciones\".",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenBatterySettings) { Text("Abrir ajustes de batería", fontSize = 12.sp) }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "Privacidad: el contenido de tus mensajes se procesa y guarda únicamente en este dispositivo. " +
                "Aló no tiene permiso de INTERNET en esta versión.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.5.sp)
            subtitle?.let {
                Text(it, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label, fontSize = 12.sp) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label, fontSize = 12.sp) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            message,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp),
        )
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
