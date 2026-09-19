# Revisión de permisos del APK de terceros

## Veredicto provisional

El APK original no debe considerarse seguro. La revisión encontró una aplicación antigua con `targetSdkVersion 27`, firma V1 בלבד y metadatos de App Cloner. El manifiesto contiene permisos amplios que no son necesarios para el modo offline, pero algunos pueden estar ligados a servicios antiguos de EA, Google, Facebook o notificaciones.

## Clasificación

| Permiso | Modo offline | Evaluación | Acción propuesta |
|---|---:|---|---|
| `INTERNET` | No estrictamente | Servicios, anuncios, login y red | Mantener solo si se necesita red |
| `ACCESS_NETWORK_STATE` | No | Comprobación de conexión | Mantener |
| `ACCESS_WIFI_STATE` | No | Detección de red | Mantener provisionalmente |
| `WAKE_LOCK` | Sí, durante partidos | Evita suspensión durante gameplay | Mantener |
| `READ_EXTERNAL_STORAGE` | Sí en Android antiguo | Lectura de recursos legacy | Mantener para compatibilidad |
| `WRITE_EXTERNAL_STORAGE` | Sí en Android antiguo | Escritura de recursos/guardados legacy | Mantener para compatibilidad |
| `VIBRATE` | No | Feedback háptico | Opcional; se puede retirar |
| `BLUETOOTH`, `BLUETOOTH_ADMIN` | No | Posible mando/dispositivo antiguo | Retirar en variante offline si no se usa mando |
| `USE_FINGERPRINT` | No | Login/identidad antiguo | Retirar |
| `GET_ACCOUNTS` | No | Login/social antiguo | Retirar |
| `MANAGE_ACCOUNTS` | No | Gestión de cuentas antigua | Retirar |
| `USE_CREDENTIALS` | No | Credenciales de cuenta antigua | Retirar |
| `READ_LOGS` | No | Permiso sensible y obsoleto | Retirar |
| `SYSTEM_ALERT_WINDOW` | No | Superposición sobre otras apps | Retirar |
| `WRITE_SETTINGS` | No | Modificación de ajustes del sistema | Retirar |
| `READ_SETTINGS` | No | Ajustes de sistema antiguos | Retirar |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | No | Excluir la app del ahorro de batería | Retirar |
| `CHANGE_WIFI_STATE` | No | Cambiar configuración Wi-Fi | Retirar |
| `net.dinglisch.android.tasker.PERMISSION_RUN_TASKS` | No | Integración con Tasker | Retirar |
| `com.android.vending.BILLING` | No | Compras dentro de aplicación | Retirar en la variante offline |
| Permisos C2DM/GCM propios | No | Push/notificaciones | Retirar junto con receivers de push |

## Motivo para no eliminar todo directamente

Las referencias a los permisos sensibles no aparecen como cadenas en `classes.dex`, pero el juego también contiene `libfifa.so`, `libnimble.so` y otros binarios nativos. Una búsqueda de cadenas no puede demostrar que una librería nativa no use una capacidad. Además, el APK declara servicios y receivers de login, push, Facebook y descarga.

Por ello, la retirada debe hacerse sobre una **copia de trabajo**, generando una variante endurecida para modo offline. El APK original debe conservarse intacto. La variante endurecida tendrá una firma nueva y puede dejar de funcionar con login, notificaciones, anuncios, compras o servicios online.

## Hallazgo adicional importante

El manifiesto incluye metadatos `com.applisto.appcloner.*`, con nombre original `FC24 MOBILE`, modelo de dispositivo clonado y opciones de modificación en tiempo de ejecución. Esto demuestra que el APK no es una distribución limpia oficial; es un APK clonado/modificado por terceros. Es una razón adicional para no certificarlo como seguro.

## VirusTotal

Se intentó consultar VirusTotal por hash y abrir la página de carga. No hay conector/API de VirusTotal configurado en la sesión y la interfaz web no expuso el formulario de carga, por lo que **no se obtuvo un veredicto de VirusTotal**. La ausencia de un resultado no equivale a una detección limpia.

## Recomendación

No instalar el APK original en el teléfono principal. La siguiente variante de trabajo debe retirar los permisos de alto riesgo y los componentes de cuenta/push, conservar los permisos mínimos de red, almacenamiento legacy y suspensión, y probarse como APK separado. No debe publicarse como oficial ni como seguro hasta probar instalación, arranque y análisis antivirus.
