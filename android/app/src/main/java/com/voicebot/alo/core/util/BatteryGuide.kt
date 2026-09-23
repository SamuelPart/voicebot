package com.voicebot.alo.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/** Instrucciones locales por fabricante; no se consulta ningún servidor. */
data class BatteryGuide(
    val manufacturer: String,
    val title: String,
    val steps: List<String>,
    val intent: Intent,
) {
    companion object {
        fun forDevice(context: Context): BatteryGuide {
            val brand = Build.MANUFACTURER.orEmpty().lowercase()
            val specific = when {
                "xiaomi" in brand || "redmi" in brand -> GuideSpec(
                    "Xiaomi / Redmi",
                    listOf("Activa Inicio automático para Aló", "Elige Ahorro de batería → Sin restricciones", "Bloquea Aló en la vista de aplicaciones recientes"),
                    "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",
                )
                "samsung" in brand -> GuideSpec(
                    "Samsung",
                    listOf("Abre Límites de uso en segundo plano", "Retira Aló de Apps en suspensión", "Añádela a Aplicaciones sin suspensión"),
                    "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
                )
                "huawei" in brand || "honor" in brand -> GuideSpec(
                    "Huawei / Honor",
                    listOf("Abre Inicio de aplicaciones", "Desactiva Gestionar automáticamente para Aló", "Activa inicio automático, secundario y ejecución en segundo plano"),
                    "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                )
                "oppo" in brand -> GuideSpec(
                    "Oppo",
                    listOf("Permite el inicio automático de Aló", "Desactiva la optimización de batería", "Permite actividad en segundo plano"),
                    "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                )
                "realme" in brand -> GuideSpec(
                    "Realme",
                    listOf("Activa inicio automático", "Permite actividad en segundo plano", "Selecciona Sin restricciones en uso de batería"),
                    "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                )
                "vivo" in brand -> GuideSpec(
                    "Vivo",
                    listOf("Abre Administrador de inicio automático", "Permite Aló", "Desactiva el ahorro de batería en segundo plano"),
                    "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
                )
                else -> GuideSpec(
                    Build.MANUFACTURER.orEmpty().ifBlank { "Android" },
                    listOf("Busca Aló en Optimización de batería", "Selecciona Sin restricciones o No optimizar", "Permite su actividad en segundo plano"),
                    null,
                )
            }
            return BatteryGuide(
                manufacturer = specific.name,
                title = "Evitar que el sistema cierre Aló",
                steps = specific.steps,
                intent = resolvableIntent(context, specific.component),
            )
        }

        private fun resolvableIntent(context: Context, component: Pair<String, String>?): Intent {
            if (component != null) {
                val candidate = Intent().setComponent(ComponentName(component.first, component.second))
                if (candidate.resolveActivity(context.packageManager) != null) return candidate
            }
            return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }

        private data class GuideSpec(
            val name: String,
            val steps: List<String>,
            val component: Pair<String, String>?,
        )
    }
}
