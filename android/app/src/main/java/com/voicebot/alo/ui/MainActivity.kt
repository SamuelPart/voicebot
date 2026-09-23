package com.voicebot.alo.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.voicebot.alo.ui.theme.AloTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AloTheme {
                val viewModel: MainViewModel = viewModel()
                val state = viewModel.permissionGranted.collectAsStateWithLifecycle()
                val settings = viewModel.settings.collectAsStateWithLifecycle()
                val entries = viewModel.logEntries.collectAsStateWithLifecycle()
                val stats = viewModel.stats.collectAsStateWithLifecycle()
                val messages = viewModel.messages.collectAsStateWithLifecycle()
                val review = viewModel.reviewQueue.collectAsStateWithLifecycle()
                val banner = viewModel.banner.collectAsStateWithLifecycle()
                val speaking = viewModel.speaking.collectAsStateWithLifecycle()
                val discarded = viewModel.discardedCount.collectAsStateWithLifecycle()

                // Al volver de los ajustes de Android, refrescamos el estado del permiso.
                DisposableEffect(Unit) {
                    viewModel.refreshPermission()
                    onDispose { }
                }

                AppScreen(
                    permissionGranted = state.value,
                    settings = settings.value,
                    entries = entries.value,
                    stats = stats.value,
                    messages = messages.value,
                    reviewQueue = review.value,
                    discardedCount = discarded.value,
                    isSpeaking = speaking.value,
                    banner = banner.value,
                    onOpenPermissionSettings = {
                        runCatching {
                            startActivity(
                                Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                        viewModel.refreshPermission()
                    },
                    onOpenBatterySettings = {
                        runCatching { startActivity(viewModel.batterySettingsIntent()) }
                    },
                    onSimulateMessage = viewModel::simulateMessage,
                    onSimulateNoise = viewModel::simulateNoise,
                    onReplay = viewModel::replay,
                    onReplayAll = viewModel::replayAllRecent,
                    onStopReading = viewModel::stopReading,
                    onMarkReviewed = viewModel::markReviewed,
                    onClearHistory = viewModel::clearHistory,
                    onConsumeBanner = viewModel::consumeBanner,
                    onToggleVoice = viewModel::setVoiceEnabled,
                    onToggleGroups = viewModel::setReadGroups,
                    onToggleHeadphones = viewModel::setOnlyWithHeadphones,
                    onTogglePauseCalls = viewModel::setPauseDuringCalls,
                    onToggleVibrate = viewModel::setVibrateInsteadOfSpeak,
                    onLanguageChange = viewModel::setLanguageTag,
                    onRetentionChange = viewModel::setRetentionDays,
                    onQuietHoursChange = viewModel::setQuietHours,
                )
            }
        }
    }
}
