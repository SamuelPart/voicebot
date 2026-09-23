package com.voicebot.alo.core.util

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telecom.TelecomManager

/**
 * Contexto del teléfono que la capa 5 usa para decidir si conviene hablar ahora.
 * Aislado en un objeto para poder simularlo en tests.
 */
object DeviceContext {

    data class Snapshot(
        val headphonesConnected: Boolean,
        val inCall: Boolean,
        val screenOn: Boolean,
    )

    fun snapshot(context: Context): Snapshot = Snapshot(
        headphonesConnected = hasHeadphones(context),
        inCall = isInCall(context),
        screenOn = isScreenOn(context),
    )

    fun hasHeadphones(context: Context): Boolean = runCatching {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d ->
                val type = d.type
                type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else {
            @Suppress("DEPRECATION")
            am.isWiredHeadsetOn
        }
    }.getOrDefault(false)

    /**
     * ¿Hay una llamada en curso?
     * Android 12+ tiene la API pública TelecomManager.isInCall(). En versiones anteriores se
     * deduce del modo de audio (IN_CALL / IN_COMMUNICATION) para no pedir READ_PHONE_STATE,
     * que es un permiso restringido por Google Play.
     */
    fun isInCall(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            tm.isInCall
        } else {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION
        }
    }.getOrDefault(false)

    private fun isScreenOn(context: Context): Boolean = runCatching {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        pm.isInteractive
    }.getOrDefault(true)
}
