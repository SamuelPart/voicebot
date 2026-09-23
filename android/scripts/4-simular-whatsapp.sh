#!/usr/bin/env bash
# El antiguo modo adb fue retirado: aceptar com.android.shell debilitaba la lista cerrada
# de paquetes. Las pruebas manuales seguras están integradas en la pantalla En vivo.
set -euo pipefail
echo "El modo de prueba por adb fue retirado."
echo "Abre Aló → En vivo y pulsa 'Probar voz' o 'Probar ruido'."
echo "Para validar captura real, envía un mensaje de WhatsApp desde otro teléfono."
