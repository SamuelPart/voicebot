# Aló — reglas ProGuard/R8
-keep class com.voicebot.alo.service.WaListenerService { *; }
-keep class * extends android.service.notification.NotificationListenerService { *; }
