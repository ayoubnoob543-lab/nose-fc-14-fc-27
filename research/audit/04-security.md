# Auditoría de seguridad — FC27

**Fecha:** 20 de septiembre de 2026  
**Alcance auditado:** launcher Android, manifiestos de distribución y actualización, scripts de preparación, archivos publicables y contenedores ZIP/OBB realmente presentes en `/home/ubuntu/work/nose-fc-14-fc-27`.  
**Método:** inspección estática de código y configuración, verificación de hashes y firmas de los binarios reales, examen de metadatos ZIP sin extracción y comprobación de los endpoints HTTPS públicos. No se modificó código, binarios ni configuración.

## Conclusión ejecutiva

El riesgo global es **crítico**. El launcher publicado es un APK **depurable y firmado con un certificado Android Debug**, mientras que el propio launcher descarga e instala software de terceros. Además, la ruta de actualización acepta un `apk_url` procedente de un JSON mutable en la rama pública `master`, pero no verifica ni el SHA-256 declarado ni la identidad del certificado del APK antes de abrir el instalador del sistema. Una toma de control de la cuenta, repositorio o rama de publicación permitiría dirigir a los usuarios a un APK arbitrario; Android mantiene la confirmación de instalación del usuario, pero el launcher le presenta la actualización como legítima.

La cadena de distribución de recursos incorpora una defensa útil: calcula SHA-256 de cada descarga y protege la extracción contra **Zip Slip**. Sin embargo, esos hashes viven en el mismo manifiesto mutable que proporciona las URLs, por lo que no proporcionan autenticidad frente a una modificación del repositorio. Los ZIP recibidos no contienen rutas de traversal, enlaces simbólicos ni una razón de compresión anómala, pero la extracción no fija límites de tamaño, número de entradas, espacio libre o colisiones con archivos existentes. El juego de terceros distribuido conserva una superficie de permisos extensa, firma heredada y componentes de origen no confiable.

| Prioridad | Hallazgo | Severidad |
|---|---|---|
| H-01 | Launcher público depurable y firmado con clave Android Debug | **Crítica** |
| H-02 | Actualización no autenticada; abre una URL arbitraria sin comprobar hash ni firmante | **Alta** |
| H-03 | Manifiestos, URLs y hashes se sirven desde una rama pública mutable sin protección | **Alta** |
| H-04 | APK de juego de terceros con permisos sensibles, SDK obsoleto y firma V1/SHA-1 | **Alta** |
| H-05 | Descarga y extracción sin límites de recursos ni transacción de staging | **Media** |
| H-06 | Verificación de artefactos de datos positiva, pero contenido ejecutable por el motor y análisis antimalware no verificable | **Media** |

## Hallazgos

### H-01 — El launcher publicado es una compilación depurable firmada con Android Debug

**Severidad: Crítica**

El APK que se entrega como `release-assets/fc27-launcher-0.2.0/FC27.apk` tiene `android:debuggable="true"` en el manifiesto compilado y está firmado por el certificado `C=US, O=Android, CN=Android Debug`. La verificación se hizo directamente contra el binario real con `aapt dump xmltree` y `apksigner verify --print-certs`; el SHA-256 calculado fue `bda30035886f11307ddad065ac43133e421486599ec2d402f80550a094c2c826`, coincidente con `release-assets/fc27-launcher-0.2.0/FC27.apk.sha256:1` y `launcher/update.json:4`.

El proyecto no define un `buildTypes.release`, una configuración de firma de producción ni un mecanismo para cargar una clave de publicación desde un secreto externo. El módulo sólo establece SDK, identificador y dependencias en `launcher/app/build.gradle:1-19`. La aplicación compilada expone también el permiso `REQUEST_INSTALL_PACKAGES`; es legítimo para la función declarada, pero aumenta notablemente la consecuencia de publicar una build de depuración (`launcher/app/src/main/AndroidManifest.xml:2-4`). Android documenta que la variante `debug` se configura como depurable y usa un keystore de depuración genérico, mientras que los artefactos de distribución requieren una firma de release gestionada de manera segura [1].

**Impacto.** Una aplicación depurable publicada reduce la resistencia frente a análisis, manipulación y depuración local. En este caso el impacto se agrava porque el launcher es un componente de suministro de software: descarga un APK y solicita su instalación (`launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt:59-77` y `88-90`). La identidad de publicación no ofrece una garantía de producción apropiada y los usuarios no pueden distinguir con confianza un artefacto de lanzamiento de uno de pruebas.

**Corrección propuesta.** Bloquear la distribución del APK actual. Definir explícitamente una variante `release` no depurable, firmarla con un keystore de producción exclusivo que no esté en el repositorio y conservar la clave y contraseñas fuera del control de versiones. Automatizar una puerta de publicación que rechace cualquier APK con `debuggable=true`, certificado cuyo sujeto contenga `Android Debug`, o esquemas de firma insuficientes. Publicar un nuevo paquete con un `versionCode` superior y verificar su certificado y manifiesto antes de volver a habilitar el canal de actualización.

### H-02 — La actualización no verifica el hash ni el firmante del APK de destino

**Severidad: Alta**

`UpdateManifest` analiza los campos `apk_url`, `sha256` y `size_bytes` (`launcher/app/src/main/java/com/nosefc27/launcher/UpdateManifest.kt:6-24`). Sin embargo, `checkForUpdate()` sólo compara la versión y entrega `manifest.apkUrl` a un `Intent.ACTION_VIEW` (`MainActivity.kt:108-115`). El campo `manifest.sha256` no se usa en ninguna ruta de actualización; tampoco se descarga el APK dentro del launcher, no se compara su tamaño, no se impone esquema HTTPS, no se limita el host y no se comprueba el certificado de firma Android o el `packageName` esperado.

La configuración publicada confirma que el manifiesto contiene un hash, por lo que el fallo no es ausencia de metadatos sino ausencia de aplicación del control: `launcher/update.json:2-7`. El launcher solicita el permiso necesario para promover instalaciones de paquetes (`AndroidManifest.xml:2-3`), y la documentación de Android define dicho permiso como la facultad de solicitar la instalación de paquetes [3].

**Impacto.** Quien pueda modificar `update.json`, sustituir su contenido servido o influir en su URL podría presentar una actualización a cualquier URI compatible. La instalación conserva la pantalla de confirmación de Android; por ello no se ha demostrado una instalación silenciosa. Aun así, un usuario que confíe en el diálogo “Actualizar FC 27” podría instalar un APK distinto del esperado. El campo SHA-256 que aparece en la interfaz de publicación no reduce este riesgo porque nunca se valida.

**Corrección propuesta.** Descargar la actualización dentro de la aplicación exclusivamente desde HTTPS, permitir sólo hosts previamente definidos, imponer un límite de tamaño y verificar el SHA-256 antes de invocar el instalador. Después, abrir el APK con `PackageManager`/`PackageInfo` y exigir el `packageName` y el certificado de firma de release esperados. Tratar el manifiesto como un documento autenticado: firmarlo con una clave offline y verificar la firma con una clave pública embebida en el launcher, o usar un canal de actualizaciones con metadatos firmados y protección contra rollback. Rechazar URL no `https`, redirecciones a host no permitido y versiones que no incrementen el `versionCode` validado.

### H-03 — La autenticidad de toda la distribución depende de una rama pública mutable y no protegida

**Severidad: Alta**

Las constantes de producción apuntan a archivos servidos desde `raw.githubusercontent.com` en la rama `master`, no a un commit inmutable, tag firmado ni manifiesto firmado: `launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt:24-28`. Ese JSON entrega simultáneamente las URLs, hashes de APK/OBB y hashes de las partes de datos (`launcher/distribution.json:2-13`). Las descargas posteriores sí calculan SHA-256 (`MainActivity.kt:82-86` y `Downloader.kt:38-45`), pero un cambio malicioso del mismo JSON puede sustituir URL y hash de forma coherente; por tanto, el hash sólo detecta errores de transporte, no autentica al emisor.

Durante esta auditoría, el repositorio remoto `ayoubnoob543-lab/nose-fc-14-fc-27` se verificó como **público** y la API de GitHub devolvió que la rama `master` no está protegida. Los archivos públicos consultados coincidían byte a byte con los locales en el momento de la comprobación: `distribution.json` SHA-256 `a99515dc48490d74774d1df3af0c9af1390bf4d9b98290ed82af36f497fdc0d4` y `update.json` SHA-256 `1d2b943a15bfa7d00aae4883fe364726d21a44149c7788a85c5528da9aab8e5a`. Esta igualdad es una observación puntual, no una garantía futura. La rama no protegida también permite que un cambio autorizado por una sola credencial comprometida alcance a los clientes tras la caché indicada por el servidor.

**Impacto.** El compromiso de una credencial con permiso de escritura, una automatización de publicación o un cambio no revisado en `master` puede reemplazar el manifiesto que gobierna la descarga de aproximadamente 6,2 GB y el flujo de actualización. Se combina directamente con H-02. El uso de TLS protege la conexión en tránsito, pero no protege frente a una alteración legítima desde el origen autorizado.

**Corrección propuesta.** No usar una rama mutable como raíz de confianza. Servir manifiestos versionados y firmados desde un canal de publicación separado; incorporar en el cliente la clave pública de verificación. Proteger la rama de publicación con revisión obligatoria, CODEOWNERS para manifiestos, CI que valide hashes y firmas, restricciones de push y cuentas con MFA/llaves de hardware. Habilitar secret scanning, push protection y actualizaciones de seguridad de dependencias en el repositorio, que se observan deshabilitados en la configuración remota. Mantener los release assets inmutables y publicar checksums firmados fuera del mismo documento que decide las URLs.

### H-04 — El APK de juego de terceros mantiene una superficie de permisos y procedencia de alto riesgo

**Severidad: Alta**

El binario publicado `release-assets/fc27-mahgames-2026-09-12/EA_SPORTS_FC_MOBILE_BETA.apk` coincide con el hash esperado `1bcea18ab5c01f5ce7e6c4cdc132a591194fe80b07d22548d66c6f74fce34b01`, declarado en `launcher/distribution.json:4-5`. La coincidencia autentica que se distribuye el archivo inventariado, pero no demuestra que éste sea seguro.

La inspección real del APK registra `targetSdkVersion 27` y una firma sólo V1; el certificado usa `SHA1withRSA`, considerado débil. La evidencia persistida está en `research/security-scan-2026-09-20/static-report.txt:11-25`. El mismo análisis enumera permisos que exceden un juego offline, entre ellos `MANAGE_ACCOUNTS`, `USE_CREDENTIALS`, `GET_ACCOUNTS`, `READ_LOGS`, `SYSTEM_ALERT_WINDOW`, `CHANGE_WIFI_STATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `WRITE_SETTINGS` y `READ_SETTINGS` (`static-report.txt:92-118`). El informe local también documenta metadatos de App Cloner y componentes de login, push, Facebook y descarga (`research/security-scan-2026-09-20/PERMISSIONS_REVIEW.md:33-45`).

**Impacto.** Estos permisos podrían permitir acceso a cuentas, superposiciones visuales, cambios de ajustes o recopilación de información si el APK los usa y el sistema/usuario los concede. La antigüedad del SDK objetivo y la firma heredada limitan la confianza en la compatibilidad de protecciones modernas. No se afirma que el APK sea malware: no se realizó ejecución dinámica, análisis de comportamiento en dispositivo ni consulta de VirusTotal; el propio informe documenta que no hubo veredicto de VirusTotal (`PERMISSIONS_REVIEW.md:43-45`). La conclusión verificable es que no debe tratarse como un binario confiable u oficial.

**Corrección propuesta.** Suspender la instalación del APK de terceros en la distribución predeterminada. Obtener una fuente verificable y autorización de distribución, o reconstruir una variante mantenida y auditable. Para una variante estrictamente offline, retirar permisos y componentes no necesarios en una copia de trabajo, firmarla con una identidad nueva y someterla a pruebas funcionales, análisis estático/dinámico y escaneo antimalware antes de publicarla. No modificar el original forense; conservar su hash y procedencia como evidencia.

### H-05 — La extracción ZIP evita traversal, pero no controla agotamiento de recursos ni sobrescrituras internas

**Severidad: Media**

La implementación es **positiva frente a Zip Slip**: construye el destino de cada entrada y exige que su ruta canónica permanezca bajo el directorio seleccionado (`MainActivity.kt:93-105`). Es el patrón recomendado por Android para impedir que una entrada `../` escriba fuera del directorio de extracción [2]. El directorio está bajo `filesDir`, por lo que no se encontró una ruta directa de escritura en almacenamiento compartido. El `FileProvider` se limita a `files/downloads/` y no está exportado (`launcher/app/src/main/res/xml/file_paths.xml:1-3` y `AndroidManifest.xml:11-13`).

La inspección de los archivos reales confirmó que `obb.zip` contiene 4 entradas, 1.43 GB comprimidos y 1.60 GB sin comprimir; `Fifa16ModFC27.zip` contiene 35.402 entradas, 5.01 GB comprimidos y 6.50 GB sin comprimir. No se hallaron rutas absolutas, `..`, enlaces simbólicos ni entradas cifradas. Esto descarta traversal en los ZIP concretos revisados, no en futuras versiones. El contenido de datos sí incluye 712 archivos `.lua`, que serán interpretables por el motor del juego; no se hallaron `.apk`, `.dex`, `.jar`, `.so`, `.exe`, `.dll` o scripts de sistema en el inventario de esos ZIP.

Persisten carencias: no hay tope de número de entradas, bytes extraídos, ratio de expansión, profundidad de ruta o espacio libre; se escribe directamente en `prepared/` y `mkdirs()` permite reutilizar un destino ya poblado (`MainActivity.kt:93-103`). Tampoco se extrae a un directorio temporal con validación final y renombrado atómico. La guía de Android recomienda exigir un destino seguro y vacío para impedir sobrescrituras accidentales y reducir riesgo de compromiso [2].

**Impacto.** Un ZIP futuro aceptado desde un manifiesto comprometido, o un archivo legítimo con tamaño inesperado, puede agotar almacenamiento, provocar denegación de servicio, mezclar contenido de ejecuciones previas o sobrescribir recursos internos preparados. La defensa actual impide escapar de `filesDir/prepared`, de modo que no se clasificó como path traversal crítico.

**Corrección propuesta.** Antes de extraer, comprobar espacio libre con margen, limitar entradas, bytes acumulados, profundidad y longitud de nombres, y rechazar tipos no requeridos. Extraer a un directorio temporal nuevo bajo almacenamiento interno, exigir que esté vacío, validar recuento/tamaño/hash esperado del conjunto y sustituir el destino mediante operación atómica o marcador de versión. Cerrar cada entrada con `closeEntry()` y eliminar el staging incompleto en errores. Mantener la comprobación canónica existente.

### H-06 — Integridad de los artefactos comprobada; análisis del contenido y controles de disponibilidad insuficientes

**Severidad: Media**

La auditoría calculó y confirmó los hashes de `FC27.apk`, `EA_SPORTS_FC_MOBILE_BETA.apk`, `obb.zip`, el ZIP de datos y sus tres partes contra los valores de `launcher/distribution.json:3-13`, `research/fc27-data-upload/DATA_PARTS_MANIFEST.json:2-23` y `release-assets/fc27-launcher-0.2.0/FC27.apk.sha256:1`. Esto demuestra integridad entre las copias locales y los manifiestos revisados. Los ZIP también pasaron la lectura de metadatos sin errores de estructura. Es una fortaleza, pero subordinada a H-03 porque los valores esperados son modificables desde el mismo canal.

`Downloader.fetch()` reserva y escribe directamente en el archivo final, permite reanudación por `Range` y sólo valida tamaño cuando el manifiesto aporta `expectedBytes` (`launcher/app/src/main/java/com/nosefc27/launcher/Downloader.kt:10-36`). La descarga de APK y OBB recibe `null` como tamaño esperado (`MainActivity.kt:61-67`); no existe límite de Content-Length, política explícita de redirecciones ni lista de esquemas/hosts. La autenticación TLS de `github.com`, `raw.githubusercontent.com` y `release-assets.githubusercontent.com` fue válida con la CA del sistema y TLS 1.3 en los dos últimos hosts al momento de la prueba. No se observó pinning de certificado, lo cual no es una vulnerabilidad por sí solo, pero el código no fuerza explícitamente HTTPS ni restringe los destinos que llegan de los JSON.

**Impacto.** Un origen controlado puede usar archivos grandes o redirecciones para consumir almacenamiento y ancho de banda antes de que el hash se calcule. La comprobación llega después de escribir el archivo completo. El contenido de datos incluye scripts Lua del motor, por lo que un hash correcto no sustituye una revisión de seguridad del contenido que el juego interpretará.

**Corrección propuesta.** Añadir `size_bytes` para todos los artefactos, rechazar respuestas sin tamaño o por encima de umbrales aprobados, descargar a `*.partial`, limitar redirecciones y permitir sólo HTTPS/hosts autorizados. Validar hash antes de promover el archivo a su nombre final. Asociar cada release a una lista de contenido firmada y a un SBOM/inventario; analizar scripts y recursos con herramientas acordes al motor antes de distribución. Establecer antivirus y pruebas dinámicas reproducibles para cada APK, dejando explícito el resultado y la fecha.

## Permisos, certificados, exposición y controles positivos

El launcher de código fuente solicita sólo `INTERNET` y `REQUEST_INSTALL_PACKAGES` (`AndroidManifest.xml:2-3`). `allowBackup` está desactivado, el `FileProvider` no se exporta y sólo comparte lectura temporal al instalador (`AndroidManifest.xml:4, 11-13`; `MainActivity.kt:88-90`). La actividad exportada es el punto de entrada `MAIN/LAUNCHER` normal (`AndroidManifest.xml:5-10`). No se encontraron claves privadas, keystores, certificados `.pem/.p12/.pfx`, secretos de API ni archivos globalmente escribibles en los archivos inspeccionados; esto no descarta secretos fuera del árbol auditado ni secretos que no coincidan con los patrones buscados.

Los manifiestos públicos y los assets de release están expuestos intencionadamente: el remoto es público y los enlaces de `launcher/distribution.json:3-12` apuntan a GitHub Releases. Los artefactos locales grandes están excluidos de Git mediante `.gitignore:2-5`, pero eso **no** reduce su exposición cuando se publican como release assets. Los permisos locales de los artefactos de origen son en general `0600`; algunas partes bajo `release-assets/` están en `0644`. No se identificó un servidor de archivos local ni una exposición adicional dentro del alcance. La revisión no puede demostrar quién posee permisos de administrador en GitHub, si existen reglas organizativas externas, ni si los tokens de publicación están protegidos fuera del repositorio.

## Limitaciones y verificaciones no concluyentes

No se ejecutaron los APK en dispositivo o emulador, no se decompilaron exhaustivamente las bibliotecas nativas y no se analizó el comportamiento de los 712 scripts Lua. Por ello, no puede afirmarse ausencia de comportamiento malicioso, telemetría, carga de código o vulnerabilidades en tiempo de ejecución. Tampoco se obtuvo una evaluación de VirusTotal, como ya documenta `research/security-scan-2026-09-20/PERMISSIONS_REVIEW.md:43-45`. La validación TLS es una observación puntual del 20 de septiembre de 2026 y no equivale a pinning ni a una garantía futura. La prueba ZIP fue de listado/metadatos en solo lectura; no se extrajeron los 6.5 GB de datos durante esta auditoría.

## Plan de remediación priorizado

1. **Inmediato:** retirar el launcher debug del canal público y desactivar el flujo de actualización hasta publicar un APK release no depurable, firmado con identidad de producción y verificado en CI.
2. **Inmediato:** bloquear la instalación automática sugerida del APK de terceros o marcarla inequívocamente como artefacto no confiable hasta completar autorización, reducción de permisos y análisis dinámico/antimalware.
3. **Corto plazo:** firmar criptográficamente los manifiestos y verificar firma, SHA-256, tamaño, HTTPS, host, paquete y certificado Android antes de instalar o extraer.
4. **Corto plazo:** proteger la rama y endurecer la publicación: revisión obligatoria, cuentas con MFA, secretos fuera de Git, release tags inmutables, secret scanning y CI de verificación.
5. **Corto plazo:** imponer cuotas y staging transaccional en descarga/extracción; conservar el control canónico de rutas ya implementado.
6. **Antes de cada release:** generar inventario/SBOM, hashes firmados, resultados reproducibles de `apksigner`/`aapt`, análisis de permisos y análisis antimalware con fecha y versión de herramienta.

## Referencias

[1]: https://developer.android.com/build/build-variants "Configure build variants | Android Developers"
[2]: https://developer.android.com/privacy-and-security/risks/zip-path-traversal "Zip Path Traversal | Android Developers"
[3]: https://developer.android.com/reference/android/Manifest.permission "Manifest.permission | Android Developers"
