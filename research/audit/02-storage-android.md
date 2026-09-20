# Auditoría de almacenamiento Android — FC27

**Identificador:** `FC27-STORAGE-02`  
**Fecha de inspección:** 20 de septiembre de 2026  
**Alcance solicitado:** `MainActivity.kt`, manifiesto de la aplicación y configuración de `FileProvider`. Se revisaron además `app/build.gradle` y los manifiestos JSON de distribución únicamente para establecer el `targetSdk`, el paquete destino y el volumen que procesa el código. **No se modificó código, no se borraron archivos y no se realizaron commits.**

## Resumen ejecutivo

El launcher está configurado con `targetSdk 35`, por lo que se ejecuta bajo el modelo de **scoped storage** y las reglas actuales de Android 14/15. La implementación descarga, ensambla y descomprime los recursos exclusivamente en el almacenamiento interno privado de `com.nosefc27.launcher`. Esta elección no necesita permisos de almacenamiento y evita exposición a otras aplicaciones, pero constituye un bloqueo funcional crítico si esos recursos deben ser consumidos por el juego distinto `com.ea.gp.fifaworld`: el código no los instala ni puede transferirlos a `Android/obb` o `Android/data` del juego.

El `FileProvider` está correctamente declarado como no exportado y concede solo lectura de forma temporal al APK que se instala. No obstante, la aplicación no comprueba ni guía el permiso especial de «instalar apps desconocidas» antes de invocar el instalador. También falta una comprobación de espacio libre y una estrategia de limpieza: con la distribución actual se duplican al menos 5,014,863,945 bytes de partes DATA al crear el ZIP ensamblado, antes de descomprimir DATA u OBB.

| Severidad global | Hallazgos |
|---|---:|
| **Crítica** | 1 crítica, 1 alta, 1 media; 1 observación de endurecimiento |

## Alcance, método y evidencia inspeccionada

La inspección estática se efectuó sobre los archivos fuente reales siguientes:

| Archivo | Uso en la auditoría |
|---|---|
| `launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt` | Descarga, escritura, ensamblado/descompresión, instalación y URI.
| `launcher/app/src/main/AndroidManifest.xml` | Permisos, configuración de `FileProvider` y exportación.
| `launcher/app/src/main/res/xml/file_paths.xml` | Raíz accesible mediante `FileProvider`.
| `launcher/app/build.gradle` | `minSdk 23`, `targetSdk 35` y `compileSdk 35`.
| `launcher/distribution.json` | Paquete del juego y tamaños de las partes, solo como contexto de impacto.
| `launcher/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` | Confirmación de que el manifiesto debug fusionado conserva proveedor, autoridad y permiso de instalación.

No se ejecutó el APK en un dispositivo Android 14 o Android 15 y no se inspeccionaron los archivos comprimidos descargados en tiempo de ejecución. En consecuencia, este informe **no puede verificar** el espacio real de cada dispositivo, el contenido/layout efectivo de `obb.zip` y DATA, la reacción concreta del instalador del fabricante, ni que el APK del juego pueda operar con los recursos una vez instalado. Sí puede verificar el flujo y las rutas que el código fuente construye.

## Base de compatibilidad Android 14/15

La configuración del módulo establece `minSdk 23` y `targetSdk 35` en `launcher/app/build.gradle:6-8`. Por tanto, no puede basarse en `requestLegacyExternalStorage`; desde Android 11 ese mecanismo se ignora para aplicaciones que apuntan a Android 11 o superior [1]. Android 14 bloquea la instalación de APKs cuyo `targetSdk` sea inferior a 23, requisito que el launcher actual cumple [2].

La aplicación no declara `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE` ni permisos `READ_MEDIA_*`; tampoco se detectó uso de `Environment`, `getExternalFilesDir`, `MediaStore`, `ACTION_OPEN_DOCUMENT`, `ACTION_OPEN_DOCUMENT_TREE` o `DocumentFile` en `MainActivity.kt`. Para las operaciones que actualmente realiza sobre su propio `filesDir`, esa ausencia es correcta: el almacenamiento interno específico de una app no requiere permisos de almacenamiento [3].

Sin embargo, la ausencia de una API SAF no habilita la transferencia a los directorios del juego. En apps que apuntan a Android 11 o superior, SAF no puede pedir selección de archivos ni árboles en `Android/data/` o `Android/obb/` [1]. No debe proponerse SAF como solución para escribir ahí.

## Hallazgos

### STOR-01 — Recursos OBB/DATA preparados en el espacio privado del launcher, inaccesibles para el juego destino

| Campo | Detalle |
|---|---|
| **Severidad** | **Crítica** |
| **Área** | Scoped storage, rutas privadas, OBB/DATA y Android 14/15 |
| **Evidencia** | `MainActivity.kt:60` crea `File(filesDir, "downloads")`; `:75-76` extrae los recursos a `File(filesDir, "prepared/obb")` y `File(filesDir, "prepared/data")`. `MainActivity.kt:77` solo cambia el botón para instalar el APK; no existe operación de copia, `MediaStore`, SAF, `PackageInstaller` de recursos ni ruta `Android/data`/`Android/obb`. `distribution.json:3` identifica el juego como `com.ea.gp.fifaworld`, mientras que `app/build.gradle:7` identifica al launcher como `com.nosefc27.launcher`. |
| **Impacto** | Las rutas efectivas quedan bajo el sandbox privado del launcher (conceptualmente `/data/user/0/com.nosefc27.launcher/files/prepared/...`). Otro paquete no puede leerlas. Android 11+ impide a una app acceder al directorio de datos interno de otra app y limita el acceso a directorios específicos de otras apps en almacenamiento externo [1]. Por ello, si el juego espera sus assets en `Android/obb/com.ea.gp.fifaworld` y/o `Android/data/com.ea.gp.fifaworld` —la finalidad que sugieren los nombres OBB/DATA—, el flujo actual no puede suministrárselos en Android 14/15. Puede terminar con una instalación aparentemente correcta pero con juego sin recursos, o sin ninguna forma soportada de completar la preparación. |
| **Corrección propuesta** | Rediseñar el empaquetado/entrega para que los assets sean propiedad del mismo paquete que los consume: por ejemplo, integrarlos en el APK/app bundle del juego, utilizar el mecanismo de descarga/expansión implementado por la propia app del juego, o añadir un componente del juego que importe contenido bajo sus propias credenciales. El launcher no debe intentar escribir mediante rutas de archivo en `Android/data` o `Android/obb` de otro paquete y tampoco debe usar SAF para ese fin, porque Android lo restringe [1]. Si el producto depende inevitablemente de un proceso administrado (ADB, MDM o equipo propietario), debe documentarlo como requisito fuera del flujo normal de una app de consumidor. |

**Detalle técnico.** La app sí puede escribir en `filesDir` sin permiso, pero esa es precisamente una ubicación diseñada para que otras apps no accedan a ella [3]. Asimismo, los ficheros almacenados en ubicaciones específicas de una app se eliminan al desinstalarla [3], de modo que incluso una solución de uso interno no aporta persistencia independiente del launcher.

### STOR-02 — Sin preflight de capacidad, presupuesto de expansión ni limpieza para una descarga y extracción multigigabyte

| Campo | Detalle |
|---|---|
| **Severidad** | **Alta** |
| **Área** | Rutas privadas, consumo de almacenamiento y robustez de extracción |
| **Evidencia** | `MainActivity.kt:60-76` conserva en `filesDir/downloads` el APK, `obb.zip`, cada parte DATA y el ZIP ensamblado, y además extrae a `filesDir/prepared/obb` y `filesDir/prepared/data`. `MainActivity.kt:69-74` crea una copia ensamblada de todas las partes, sin borrar las fuentes después. `MainActivity.kt:93-105` descomprime sin calcular tamaño expandido, número de entradas ni cuota disponible. No hay llamadas a `StorageManager`, `getAllocatableBytes`, `StatFs`, limpieza ni reserva de espacio. `distribution.json:10-12` declara tres partes DATA que suman **5,014,863,945 bytes**; solo coexistir partes + ensamblado ya requieren al menos **10,029,727,890 bytes** (≈10.03 GB decimales), antes del ZIP OBB, el APK y ambos árboles extraídos. |
| **Impacto** | Es probable que dispositivos con poca capacidad fallen durante ensamblado o extracción, después de descargar muchos GB. La excepción se muestra como texto genérico en `MainActivity.kt:78`, pero no recupera espacio ni permite un reintento limpio. El usuario puede quedar con contenido parcial ocupando el almacenamiento interno privado. El tamaño descomprimido no está acotado por el código, por lo que un cambio legítimo pero muy comprimido en los artefactos puede incrementar mucho la demanda. La guía de Android indica consultar espacio disponible antes de escribir archivos específicos de la app [3]. |
| **Corrección propuesta** | Antes de iniciar, calcular un presupuesto conservador: descargas pendientes + copia ensamblada + tamaño descomprimido de DATA/OBB + margen transaccional. Publicar tamaños comprimidos y descomprimidos autenticados en el manifiesto de distribución. Consultar espacio con `StorageManager.getAllocatableBytes()` (y, si procede, solicitar liberación mediante los flujos de sistema), rechazar la operación si no alcanza, y explicar el mínimo real. Escribir en archivos temporales, validar hashes, renombrar de forma atómica, eliminar de inmediato partes y ZIPs ya consumidos, y eliminar resultados parciales ante error/cancelación. Además, limitar entradas, bytes extraídos y ratios de expansión durante `extractZip`. |

**Nota sobre seguridad de ZIP.** `MainActivity.kt:98-103` hace una comprobación de `canonicalPath` frente a la ruta destino, por lo que existe una defensa explícita frente a Zip Slip. Este informe no ha encontrado una evasión en ese fragmento. El problema de este hallazgo es la cuota/recuperación, no traversal de rutas.

### STOR-03 — El flujo de instalación no verifica la autorización especial de «orígenes desconocidos» ni dirige a sus ajustes

| Campo | Detalle |
|---|---|
| **Severidad** | **Media** |
| **Área** | URI permissions, instalación de APK y compatibilidad Android 14/15 |
| **Evidencia** | El manifiesto declara `android.permission.REQUEST_INSTALL_PACKAGES` en `AndroidManifest.xml:3`. `MainActivity.kt:88-91` construye una URI `content://` con `FileProvider` e invoca directamente `ACTION_VIEW`. No hay llamada a `PackageManager.canRequestPackageInstalls()` ni uso de `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES` en el archivo inspeccionado. |
| **Impacto** | Declarar el permiso no equivale a que el usuario haya permitido al launcher instalar desde esa fuente. Si la autorización especial está desactivada, el flujo puede ser bloqueado o acabar en una pantalla de sistema sin una guía controlada por la app. Esto perjudica el recorrido de instalación en Android 14/15, aunque no supone una exposición adicional de archivos. `PackageManager.canRequestPackageInstalls()` existe específicamente para comprobar si el paquete llamante puede solicitar instalaciones [4], y `ACTION_MANAGE_UNKNOWN_APP_SOURCES` abre el ajuste para una fuente de app concreta desde API 26 [5]. |
| **Corrección propuesta** | Antes de `installApk`, comprobar `packageManager.canRequestPackageInstalls()` en API 26+. Si devuelve `false`, informar de forma clara y abrir `Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))`; al retorno, volver a comprobar antes de abrir el instalador. Conservar `REQUEST_INSTALL_PACKAGES` solo si esta función es esencial y compatible con la política de distribución aplicable. Manejar también falta de activity/resolvedor y resultado cancelado. |

## Revisión de FileProvider y permisos URI

La configuración actual de `FileProvider` es apropiada para el APK local y no aparece como hallazgo de exposición directa:

| Control revisado | Evidencia | Resultado |
|---|---|---|
| Autoridad coherente | `AndroidManifest.xml:11` usa `${applicationId}.files`; `MainActivity.kt:89` usa `${BuildConfig.APPLICATION_ID}.files`. El manifiesto fusionado debug resuelve ambos a `com.nosefc27.launcher.files` (`build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml:38-45`). | **Correcto.** Evita desajuste de autoridad.
| Proveedor no disponible libremente | `AndroidManifest.xml:11` tiene `android:exported="false"`. | **Correcto.** No es un proveedor públicamente navegable.
| Concesión por URI | `AndroidManifest.xml:11` activa `android:grantUriPermissions="true"`; `MainActivity.kt:90` añade solo `FLAG_GRANT_READ_URI_PERMISSION`. | **Correcto.** La app receptora recibe lectura temporal, no escritura. Este es el patrón documentado para compartir archivos internos mediante `FileProvider` [6].
| Raíz declarada | `file_paths.xml:2` limita el proveedor a `<files-path name="downloads" path="downloads/" />`. El APK de `MainActivity.kt:61` está dentro de esa raíz. Los directorios `prepared/obb` y `prepared/data` no están expuestos por el proveedor. | **Correcto para el flujo actual.** `getUriForFile` solo puede devolver URI de rutas declaradas [6].
| Datos de juego | `MainActivity.kt:75-76` escribe bajo `files/prepared`, fuera de `file_paths.xml:2`. | **Positivo en confidencialidad:** no se concede accidentalmente una URI de los árboles preparados.

### OBS-STOR-04 — Endurecimiento recomendado: la raíz FileProvider incluye todo `downloads/`, no solo el APK

| Campo | Detalle |
|---|---|
| **Severidad** | **Baja / defensa en profundidad** |
| **Evidencia** | `file_paths.xml:2` declara la carpeta completa `downloads/`. Esa carpeta contiene `FC27-game.apk`, `obb.zip`, las partes DATA y el ZIP ensamblado según `MainActivity.kt:60-73`; el código actual solo llama a `getUriForFile` para el APK en `:89`. |
| **Impacto** | No hay exposición explotable demostrada en el código auditado: el proveedor no está exportado y el único grant observado es de lectura del APK. Aun así, una futura llamada que convierta otro archivo de `downloads/` a URI podría conceder accidentalmente acceso a artefactos de juego de gran tamaño. |
| **Corrección propuesta** | Separar el APK instalable de los artefactos temporales (por ejemplo, un subdirectorio `install/`) y declarar en `file_paths.xml` una ruta estrecha para ese subdirectorio o el archivo de instalación. Mantener `exported="false"`, `grantUriPermissions="true"` y solo `FLAG_GRANT_READ_URI_PERMISSION`. |

## SAF, OBB/DATA y permisos de almacenamiento: veredicto explícito

| Pregunta | Veredicto verificable |
|---|---|
| ¿Usa almacenamiento compartido o rutas directas a `Android/data` / `Android/obb`? | **No.** No hay referencia a esas rutas o a almacenamiento externo en `MainActivity.kt`; se usa `filesDir`.
| ¿Necesita permisos clásicos de almacenamiento para el flujo actual? | **No**, porque solo opera sobre el almacenamiento interno específico del launcher [3].
| ¿Puede el launcher, tal como está, colocar recursos en el espacio privado del juego distinto? | **No.** No hay código que lo intente y Android restringe el acceso entre apps [1].
| ¿SAF permite corregir la copia a `Android/data` o `Android/obb` del juego? | **No.** En apps dirigidas a Android 11+, SAF no permite solicitar esos directorios [1].
| ¿Se declara `MANAGE_EXTERNAL_STORAGE`? | **No.** No se ha detectado en el manifiesto. Esto evita un permiso de acceso amplio, pero tampoco resuelve la entrega de assets a otro paquete.
| ¿El APK se comparte mediante `file://`? | **No.** Se usa una `content://` URI de `FileProvider` y grant temporal de lectura, que es el patrón correcto [6].
| ¿Es totalmente compatible el flujo en Android 14/15? | **No.** Es compatible para escribir en `filesDir` y compartir el APK por `FileProvider`, pero no resuelve la entrega de OBB/DATA al paquete diferente y no gestiona el ajuste de fuentes desconocidas. No se hizo prueba física en Android 14/15. |

## Priorización de remediación

1. **Bloquear la liberación funcional hasta resolver STOR-01.** Definir una arquitectura de distribución soportada donde el juego dueño de los assets los obtenga en su propio almacenamiento. No implementar una copia por rutas a `Android/data`/`Android/obb` ni una falsa solución SAF.
2. **Corregir STOR-02 antes de distribuir descargas de producción.** Incorporar presupuesto de espacio, temporales, cuotas de extracción y limpieza recuperable.
3. **Corregir STOR-03 antes de la siguiente versión.** Añadir la comprobación y navegación controlada a la autorización de fuentes desconocidas, con recuperación de errores.
4. **Aplicar OBS-STOR-04 como endurecimiento.** Reducir la raíz FileProvider al archivo/subdirectorio estrictamente instalable.
5. Realizar pruebas instrumentadas en dispositivos reales Android 14 y Android 15: almacenamiento justo por encima/debajo del presupuesto, cancelación y reanudación, cambio de autorización de fuente, instalador sin resolvedor, desinstalación del launcher y verificación de que el juego recibe los assets mediante la arquitectura final.

## Referencias

[1]: https://developer.android.com/about/versions/11/privacy/storage "Android Developers — Storage updates in Android 11"
[2]: https://developer.android.com/about/versions/14/behavior-changes-all "Android Developers — Behavior changes: all apps, Android 14"
[3]: https://developer.android.com/training/data-storage/app-specific "Android Developers — Access app-specific files"
[4]: https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls() "Android Developers — PackageManager.canRequestPackageInstalls"
[5]: https://developer.android.com/reference/android/provider/Settings#ACTION_MANAGE_UNKNOWN_APP_SOURCES "Android Developers — Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES"
[6]: https://developer.android.com/reference/androidx/core/content/FileProvider "Android Developers — FileProvider"
