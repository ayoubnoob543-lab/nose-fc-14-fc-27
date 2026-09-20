# Auditoría de actualizaciones — FC27

**Fecha:** 20 de septiembre de 2026  
**Alcance:** `launcher/app/src/main/java/com/nosefc27/launcher/UpdateManifest.kt`, `launcher/update.json`, flujo de intentos de instalación, firma, downgrade, HTTPS y preservación de datos.  
**Resultado global:** **ALTO**

## Conclusión ejecutiva

El mecanismo de actualización del launcher no es apto para una distribución de producción. El manifiesto anuncia un hash SHA-256 y un tamaño, pero el flujo de actualización no descarga el APK ni comprueba ninguno de esos dos valores. En su lugar, entrega la URL controlada por el manifiesto a un `ACTION_VIEW`. La firma de paquete de Android sigue siendo una barrera para sustituir una actualización *in-place* por un APK con otro certificado, pero el launcher no limita el paquete de destino ni verifica el artefacto antes de abrirlo. Por ello, una alteración del manifiesto alojado en la rama mutable `master` puede llevar al usuario a instalar un APK distinto tras la interacción del instalador.

También hay una inconsistencia que bloquea el propio flujo: el binario publicado como `0.2.0` declara realmente `versionName=0.1.0` y `versionCode=1`, mientras que el código compara el manifiesto con la constante manual `CURRENT_VERSION = "0.2.0"`. No se compara el `versionCode` de Android ni se enlaza el estado mostrado a `BuildConfig`; por tanto, no existe una garantía de que la actualización anunciada sea instalable ni de que no sea un downgrade. El APK de lanzamiento examinado está firmado con un certificado **Android Debug** y el proyecto no contiene una configuración de firma de release.

Los endpoints presentes usan HTTPS y la comprobación externa realizada en esta auditoría validó la cadena TLS por defecto. No se encontró una excepción de tráfico en claro ni un `TrustManager`/`HostnameVerifier` personalizado. Sin embargo, no hay validación del esquema, host, redirecciones o firma del manifiesto, ni pinning de certificado. La protección de transporte por sí sola no convierte un JSON servido desde `master` en un manifiesto inmutable y autenticado para la aplicación.

## Hallazgos priorizados

| ID | Severidad | Hallazgo |
|---|---|---|
| UPD-01 | Alta | El hash y el tamaño del APK declarados en `update.json` nunca se verifican antes de la instalación. |
| UPD-02 | Alta | La versión del manifiesto, la constante de aplicación y el APK publicado son inconsistentes; no se usa `versionCode` y no hay control real de downgrade. |
| UPD-03 | Alta | El APK publicado está firmado con certificado de depuración y no hay configuración de firma de release verificable. |
| UPD-04 | Media | Los endpoints actuales son HTTPS, pero la URL remota no tiene validación de esquema/host ni manifiesto firmado; la rama `master` es mutable. |
| UPD-05 | Media | No hay estrategia de migración o recuperación de datos; `allowBackup=false` deja los recursos privados sin restauración ante una reinstalación. |
| UPD-06 | Baja | El intent de actualización no controla la instalación ni reporta el resultado; el intent del juego sí limita la URI al `FileProvider`, pero el de actualización no. |

## Hallazgos detallados

### UPD-01 — El control de integridad anunciado no se ejecuta

**Severidad: Alta.** `UpdateManifest` deserializa los campos `sha256` y `size_bytes`, pero no valida formato, presencia útil ni rango. Más importante, `checkForUpdate()` solo usa `version`, `notes`, `sizeBytes`, `mandatory` y `apkUrl`: al confirmar, abre directamente `Intent(Intent.ACTION_VIEW, Uri.parse(manifest.apkUrl))`. No invoca `Downloader.fetch()`, `Downloader.sha256()` ni compara el tamaño. En contraste, el flujo separado de descarga del juego sí descarga localmente y compara los hashes SHA-256 antes de presentar la instalación.

**Evidencia.** `launcher/app/src/main/java/com/nosefc27/launcher/UpdateManifest.kt:18-23` lee `apk_url`, `sha256` y `size_bytes`. `launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt:108-115` procesa la actualización y abre la URL en la línea 113. `MainActivity.kt:82-85` muestra la rutina de descarga con hash que no se llama desde ese flujo. `launcher/update.json:3-5` publica URL, hash y tamaño para el APK de actualización.

**Impacto.** La apariencia de integridad en el manifiesto no protege el APK entregado. Quien pueda cambiar el manifiesto de la rama `master`, o comprometer el origen del manifiesto, puede proporcionar otra URI. Si la URI apunta a un APK con el mismo paquete y certificado, Android puede aceptar la actualización si además su `versionCode` es mayor. Si apunta a otro paquete, el `ACTION_VIEW` puede mostrar al usuario una instalación independiente. El instalador de Android exige compatibilidad de certificado para actualizar una aplicación existente, lo cual reduce el alcance de una sustitución *in-place*, pero no sustituye la validación del artefacto por parte del launcher. Android documenta que una actualización se admite cuando los certificados del APK nuevo y del instalado coinciden.[1]

**Corrección propuesta.** Descargar el APK de actualización a un archivo temporal en almacenamiento privado mediante el mismo patrón usado para los recursos. Antes de generar el intent, validar estrictamente que el manifiesto contiene un SHA-256 hexadecimal de 64 caracteres, un tamaño positivo y una URL `https` con host permitido; comprobar tanto tamaño como SHA-256 del archivo descargado. Inspeccionar además el APK con `PackageManager.getPackageArchiveInfo()` y exigir el `applicationId` esperado, un `versionCode` superior y certificado de firma compatible con el instalado. Servir el manifiesto con una firma de aplicación (por ejemplo, Ed25519 con clave pública embebida) o un marco de actualización con metadatos firmados; usar HTTPS como capa de transporte, no como único mecanismo de autenticidad.

### UPD-02 — La política de versión no está unida al binario y permite fallos de actualización/downgrade

**Severidad: Alta.** El proyecto define `versionCode 1` y `versionName '0.1.0'` en Gradle. El APK local que corresponde exactamente al hash publicado en `update.json` fue inspeccionado con `aapt`: declara `package=com.nosefc27.launcher`, `versionCode=1` y `versionName=0.1.0`. Sin embargo, el metadato de publicación y `update.json` lo denominan `0.2.0`, y el código compara todo manifiesto con la constante manual `CURRENT_VERSION = "0.2.0"`.

**Evidencia.** `launcher/app/build.gradle:6-8` fija `versionCode 1` y `versionName '0.1.0'`. `launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt:25` fija `CURRENT_VERSION = "0.2.0"`; las líneas 110-111 comparan ese valor y no `BuildConfig.VERSION_NAME` ni `BuildConfig.VERSION_CODE`. `launcher/update.json:2` anuncia `0.2.0`. `research/fc27-launcher-0.2.0.json:2-7` identifica el binario como versión `0.2.0` y como `debug-signed test build`. Durante esta auditoría, `sha256sum` confirmó que `release-assets/fc27-launcher-0.2.0/FC27.apk` coincide con el hash de `update.json`, y `aapt dump badging` devolvió el `versionCode` y `versionName` indicados arriba.

**Impacto.** Un launcher instalado desde el APK publicado se identifica internamente como `0.2.0` y no mostrará la actualización cuyo manifiesto también dice `0.2.0`, aunque el paquete Android sea `0.1.0`. Para futuras versiones, el texto semántico mayor solo activa un diálogo; no demuestra que el APK incluya un `versionCode` mayor que 1. Android puede rechazar el intento como downgrade después de que el usuario haya iniciado la instalación. No se observó una ruta de código que habilite explícitamente downgrades, por lo que **no se verificó un downgrade silencioso**; el defecto comprobado es la ausencia de validación previa y la posibilidad de un fallo de actualización por una publicación incoherente.

**Corrección propuesta.** Eliminar `CURRENT_VERSION` manual y comparar el manifiesto con el estado instalado real (`BuildConfig.VERSION_CODE`/`PackageInfo.longVersionCode`). Añadir un campo numérico obligatorio `version_code` al manifiesto y exigir que sea estrictamente superior al instalado; conservar `version` solo como texto de presentación. En el pipeline de release, fallar la publicación si el `versionName`, `versionCode`, etiqueta, hash, tamaño y APK no coinciden. Añadir pruebas unitarias para `isNewerVersion` y pruebas instrumentadas que cubran igual versión, versión menor y `versionCode` mayor/menor.

### UPD-03 — El artefacto público está firmado en modo debug; falta una identidad de firma de producción verificable

**Severidad: Alta.** El archivo publicado `release-assets/fc27-launcher-0.2.0/FC27.apk` pasó la verificación `apksigner` con esquemas v1 y v2, pero su único firmante es `C=US, O=Android, CN=Android Debug`. El certificado tiene huella SHA-256 `e4335a4050fd8497da205f48d510ee4591da8bb40c664dff8bdc50778bcc7859`. El metadato local confirma literalmente `build_variant: "debug-signed test build"`. En el archivo Gradle de la app no hay bloque `signingConfigs`, `signingConfig` ni referencia a una clave de release. La auditoría no encontró keystore, certificado de release ni configuración de firma dentro del repositorio.

**Evidencia.** Resultado de `apksigner verify --verbose --print-certs release-assets/fc27-launcher-0.2.0/FC27.apk` durante la auditoría: un firmante Android Debug, v1/v2 válidos. `research/fc27-launcher-0.2.0.json:3-7` marca el binario como `debug-signed test build`. `launcher/app/build.gradle:1-19` contiene la configuración completa de la app y no declara una firma de release.

**Impacto.** Un certificado de depuración no es una identidad de lanzamiento controlada y estable. Si una versión futura se firma con otra clave, Android la tratará como otra aplicación o rechazará la actualización sobre el paquete instalado; la continuidad de datos dependerá entonces de una desinstalación/reinstalación. La documentación de Android exige conservar el mismo certificado durante la vida esperada de la app para que las actualizaciones sean posibles.[1] No se pudo verificar la custodia de ninguna clave fuera del repositorio, por lo que no se afirma que la clave actualmente usada esté expuesta; sí se confirma que el artefacto distribuido es de depuración.

**Corrección propuesta.** Establecer una clave de release dedicada, guardada fuera del repositorio en un gestor de secretos o HSM, y configurar la firma de release mediante variables seguras del entorno de CI. Publicar solo artefactos release firmados y registrar/fijar la huella SHA-256 de su certificado en el proceso de entrega. Antes de migrar desde el APK debug ya distribuido, diseñar y probar el camino de transición: una aplicación firmada con otra clave no puede actualizarlo directamente. No almacenar keystores ni contraseñas en Git.

### UPD-04 — HTTPS está presente, pero no hay política de confianza de URL ni autenticación del manifiesto

**Severidad: Media.** Las dos URLs codificadas y la URL de APK actual usan `https`. La prueba de red de esta auditoría obtuvo HTTP 200 y `ssl_verify_result=0` para el manifiesto de `raw.githubusercontent.com`; el enlace de descarga del APK redirigió una vez a `release-assets.githubusercontent.com` y también finalizó en HTTPS con verificación TLS correcta. En el código no se encontró `TrustManager`, `HostnameVerifier`, `SSLSocketFactory`, `networkSecurityConfig` ni `usesCleartextTraffic` que deshabilite la validación por defecto. Sin embargo, `UpdateManifest.fromJson()` acepta `apk_url` sin comprobar esquema ni host, y `checkForUpdate()` lo entrega a un intent. Tampoco hay pinning ni firma del JSON. El manifiesto proviene de la rama mutable `master`.

**Evidencia.** `MainActivity.kt:26-27` fija los manifiestos en `https://raw.githubusercontent.com/.../master/...`; `MainActivity.kt:119` usa `HttpURLConnection` con sus validaciones TLS por defecto. `UpdateManifest.kt:15-24` solo extrae cadenas del JSON. `MainActivity.kt:113` usa la URL no validada. `launcher/update.json:3` apunta a una descarga HTTPS de GitHub. La inspección estática no encontró una configuración de seguridad de red ni anulación de validación TLS bajo `launcher/app`.

**Impacto.** Los valores actualmente comprometidos usan HTTPS, por lo que no se verificó tráfico en claro en el estado auditado. Aun así, el origen del manifiesto puede cambiar su contenido sin que el binario detecte la modificación, y un `apk_url` futuro podría ser `http`, otro host HTTPS o una cadena de redirecciones no prevista. Esto amplía la superficie del hallazgo UPD-01 y evita una política auditable de fuentes de actualización.

**Corrección propuesta.** Mantener el tráfico en HTTPS y rechazar explícitamente cualquier esquema distinto de `https`; aplicar una lista de hosts permitidos y validar la URL final tras redirecciones. Publicar manifestos en rutas versionadas e inmutables, no en una rama mutable. Firmar los metadatos de actualización y verificar esa firma en el cliente. El pinning de certificado puede añadir defensa frente a determinados compromisos de CA, pero debe implementarse solo con un plan de rotación; no reemplaza la firma del manifiesto. La autorización `REQUEST_INSTALL_PACKAGES` solo permite solicitar instalaciones y no equivale a instalación silenciosa.[2]

### UPD-05 — La actualización normal no borra datos en el código, pero no existe recuperación ante una reinstalación ni migración explícita

**Severidad: Media.** No se encontró código que borre los directorios del launcher durante `checkForUpdate()` o antes del intent de actualización. El contenido preparado se guarda bajo `filesDir` (`prepared/obb` y `prepared/data`), que normalmente se conserva cuando Android aplica una actualización del mismo paquete y certificado. Las únicas eliminaciones localizadas son `assembled.delete()` antes de reconstruir un ZIP y `target.delete()` cuando una descarga parcial recibe una respuesta HTTP completa; ambas pertenecen al flujo de preparación de recursos, no a la actualización del launcher.

No obstante, la aplicación declara `android:allowBackup="false"`. No hay reglas de extracción, migración de esquema, exportación de estado, comprobación posterior a la instalación ni documentación en el código para recuperar las descargas si el usuario debe desinstalar y reinstalar, por ejemplo tras un cambio de certificado de firma. Android Auto Backup incluye por defecto archivos de `getFilesDir()`, pero puede deshabilitarse con `allowBackup=false`; esta aplicación lo deshabilita.[3] Por tanto, una reinstalación eliminaría los recursos privados sin una recuperación preparada por esta implementación.

**Evidencia.** `launcher/app/src/main/AndroidManifest.xml:4` fija `android:allowBackup="false"`. `MainActivity.kt:60-76` escribe APK, OBB y DATA en `filesDir/downloads` y `filesDir/prepared`. `MainActivity.kt:69-70` borra únicamente el ZIP ensamblado previo; `Downloader.kt:12-19` borra un destino parcial solo al reanudar contra una respuesta 200. `MainActivity.kt:108-116` no contiene una operación de limpieza ni de migración durante la actualización.

**Impacto.** Para una actualización legítima *in-place* y firmada con el mismo certificado, el código inspeccionado no evidencia pérdida de datos. Esa preservación **no se validó en un dispositivo Android real**. Si la actualización se convierte en reinstalación —un riesgo reforzado por UPD-03— se perderán los contenidos privados potencialmente voluminosos y el launcher no ofrece restauración ni estado de reanudación. Habilitar Auto Backup sin diseño previo tampoco resolvería el problema: Android limita Auto Backup a 25 MB por usuario y el proyecto indica recursos de aproximadamente 6,2 GB.[3]

**Corrección propuesta.** Garantizar primero actualizaciones *in-place* con la misma identidad de firma. Diseñar migraciones versionadas e idempotentes, conservar los recursos existentes hasta que la actualización y su verificación hayan concluido, y mostrar claramente que el usuario no debe desinstalar para actualizar. Para recursos grandes, implementar una recuperación/reanudación propia con manifiestos de archivos verificados, más una exportación opcional del estado mínimo del usuario; no depender de Auto Backup para varios gigabytes. Probar en dispositivo las rutas de actualización, cancelación, falta de espacio, fallo de firma y reinstalación.

### UPD-06 — Los intents de instalación no son homogéneos y la actualización no recibe resultado

**Severidad: Baja.** Para instalar el APK del juego descargado, el launcher usa un `FileProvider` no exportado con una ruta limitada a `files/downloads`, URI `content://`, MIME de APK y permiso temporal de lectura. Esta parte está acotada correctamente. Para actualizar el launcher, en cambio, abre una URI remota con `ACTION_VIEW`, no fija MIME, no confirma capacidad de instalar paquetes, no observa el resultado y no vuelve a comprobar la versión instalada. Tampoco usa una sesión de `PackageInstaller` ni una descarga propia.

**Evidencia.** `AndroidManifest.xml:3` solicita `REQUEST_INSTALL_PACKAGES`. `AndroidManifest.xml:11-13` declara un `FileProvider` no exportado; `res/xml/file_paths.xml:1-3` lo restringe a `downloads/`. `MainActivity.kt:88-91` instala el juego con URI de `FileProvider`, tipo MIME y `FLAG_GRANT_READ_URI_PERMISSION`. `MainActivity.kt:113` abre el APK de actualización por URL remota, sin receptor de resultado.

**Impacto.** Si el permiso especial de orígenes desconocidos no está concedido, si no hay manejador adecuado o si el instalador rechaza el APK por firma/versión, el launcher no ofrece un diagnóstico ni actualiza su estado. La instalación sigue requiriendo interacción del usuario; no se halló instalación silenciosa. El defecto reduce control y trazabilidad, y permite que la experiencia de actualización dependa de un manejador externo.

**Corrección propuesta.** Tras verificar el APK local conforme a UPD-01, usar el `FileProvider` limitado para el launcher también. Antes de iniciar la instalación, comprobar la capacidad pertinente del gestor de paquetes y guiar al usuario al ajuste necesario. Preferir `PackageInstaller` con una sesión y un receptor de estado, o manejar de forma explícita los resultados y errores del instalador. Tras el retorno a la app, volver a leer `PackageInfo` y confirmar que paquete, certificado y `versionCode` esperados quedaron instalados.

## Controles que sí se verificaron

El hash `bda30035886f11307ddad065ac43133e421486599ec2d402f80550a094c2c826` de `launcher/update.json` coincidió con el archivo local `release-assets/fc27-launcher-0.2.0/FC27.apk`; el problema es que la app no reproduce esa comprobación para la actualización. `apksigner` validó firmas v1 y v2 del APK. El `FileProvider` está declarado `exported="false"` y solo expone `downloads/`. Las URLs presentes en el estado auditado son HTTPS y la comprobación TLS externa no reportó error. No se halló una ruta de downgrade silencioso, una llamada de borrado de datos durante el flujo de actualización ni una anulación explícita de la validación TLS.

## Límites de verificación

La revisión fue estática sobre los archivos y APK locales reales, más una comprobación HTTP/TLS de los endpoints publicados. No se instaló ni actualizó una aplicación en un dispositivo o emulador, por lo que no se verificaron empíricamente la preservación de `filesDir`, el comportamiento concreto del instalador del fabricante, la concesión de orígenes desconocidos ni las rutas de migración. Tampoco es posible inferir desde el repositorio la custodia de claves, permisos de GitHub, controles del servidor o futuras firmas; esos aspectos se declaran explícitamente como no verificados, no como ausentes fuera del árbol inspeccionado.

## Referencias

[1]: https://developer.android.com/studio/publish/app-signing "Sign your app"
[2]: https://developer.android.com/reference/android/Manifest.permission#REQUEST_INSTALL_PACKAGES "Android Manifest.permission: REQUEST_INSTALL_PACKAGES"
[3]: https://developer.android.com/identity/data/autobackup "Back up user data with Auto Backup"
