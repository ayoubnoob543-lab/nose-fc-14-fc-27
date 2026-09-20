# Auditoría 05 — Build Android de FC27

**Fecha de auditoría:** 20 de septiembre de 2026  
**Alcance:** exclusivamente `launcher/`: Gradle, `AndroidManifest.xml`, recursos, SDK mínimos/objetivo, `BuildConfig`, `FileProvider` y reproducibilidad del APK. No se modificó código, no se borraron archivos y no se hicieron commits. El único archivo creado en el proyecto es este informe.

## Conclusión ejecutiva

El launcher **compila de forma determinista en el entorno auditado**: dos compilaciones limpias desde el contenido versionado de `HEAD:launcher`, realizadas en directorios aislados, produjeron el mismo SHA-256 que el APK conservado en `release-assets/fc27-launcher-0.2.0/FC27.apk`: `bda30035886f11307ddad065ac43133e421486599ec2d402f80550a094c2c826`.

Sin embargo, el artefacto publicado es inequívocamente un **APK debug**: está marcado como depurable y firmado con el certificado estándar `Android Debug`. No debe distribuirse como release. Además, la identidad de versión del APK (`1` / `0.1.0`) no coincide con la versión que usa el código y los manifiestos de actualización (`0.2.0`). El proceso puede reproducirse hoy con la herramienta local documentada, pero no queda autosuficientemente reproducible a largo plazo porque el repositorio no contiene Gradle Wrapper, bloqueo/verificación de dependencias ni automatización de compilación.

| Resultado | Estado | Evidencia principal |
|---|---|---|
| Compilación limpia | **Verificada** | Dos builds aislados con Gradle 8.10.2 y Android API/build-tools 35 generaron el mismo hash que el APK conservado. |
| Artefacto distribuible | **No apto** | `BuildConfig.DEBUG=true`, `android:debuggable=true` y certificado `CN=Android Debug`. |
| Versionado de actualización | **Incoherente** | APK/`BuildConfig`: `0.1.0`; código y JSON: `0.2.0`. |
| FileProvider | **Correcto en alcance** | No exportado, URI grants y ruta limitada a `files/downloads/`. |
| SDK | **Verificado** | `minSdk 23`, `targetSdk 35`, `compileSdk 35`, coherentes con el APK empaquetado. |
| Recursos propios | **Insuficientes para publicación** | Sólo `styles.xml` y `file_paths.xml`; no hay icono explícito, recursos adaptativos, cadenas ni localizaciones propias. |

## Hallazgos priorizados

### BLD-05-01 — El APK de release conservado es una compilación debug firmada con la clave de depuración

**Severidad: Crítica**

El APK que se conserva como `release-assets/fc27-launcher-0.2.0/FC27.apk` tiene exactamente el mismo SHA-256 que `launcher/app/build/outputs/apk/debug/app-debug.apk`: `bda30035886f11307ddad065ac43133e421486599ec2d402f80550a094c2c826`. Por tanto, el archivo denominado como release no es una variante release distinta, sino el producto debug.

La configuración del módulo no declara `buildTypes` ni `signingConfigs`; sólo configura el bloque común Android. El `BuildConfig` generado para el artefacto contiene `DEBUG = true` y `BUILD_TYPE = "debug"`. El manifiesto final también contiene `android:debuggable="true"`. La verificación de firma con `keytool -printcert -jarfile` devolvió `Owner: C=US, O=Android, CN=Android Debug` e `Issuer: C=US, O=Android, CN=Android Debug`.

**Evidencia.**

| Archivo o artefacto | Línea / dato verificado |
|---|---|
| `launcher/app/build.gradle` | 6–10: no existen `buildTypes` ni `signingConfigs`; se habilita `buildConfig`. |
| `launcher/app/build/generated/source/buildConfig/debug/com/nosefc27/launcher/BuildConfig.java` | 7–11: `DEBUG=true`, `BUILD_TYPE="debug"`, `VERSION_CODE=1`, `VERSION_NAME="0.1.0"`. |
| `launcher/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml` | 20–27, especialmente 23: `android:debuggable="true"`. |
| `launcher/app/build/outputs/apk/debug/app-debug.apk` | Firma: certificado `Android Debug`; hash `bda300…c826`. |
| `release-assets/fc27-launcher-0.2.0/FC27.apk` | Mismo hash `bda300…c826`, tamaño 6.057.099 bytes. |

**Impacto.** Un APK depurable facilita la inspección y depuración en un dispositivo de usuario, no representa una cadena de firma de producción y no establece una identidad estable para actualizaciones futuras. Cuando se instale una release firmada con una clave de producción diferente, Android no podrá actualizar sobre instalaciones firmadas con la clave debug; exigirá desinstalar, lo que puede eliminar los datos privados del launcher. El nombre `FC27.apk` y su ubicación bajo `release-assets` incrementan el riesgo de que se publique por error.

**Corrección propuesta.** Definir una variante `release` no depurable y un `signingConfig` de producción. La clave no debe almacenarse en Git: cargarla desde secretos de CI o un almacén de secretos, y configurar el build con propiedades inyectadas. Generar el APK/AAB con `:app:assembleRelease` o el flujo de distribución elegido, ejecutar `apksigner verify --verbose --print-certs`, conservar la huella de certificado de producción y publicar únicamente el artefacto de release. Antes de reemplazar el artefacto, incrementar `versionCode` y alinear `versionName` con la versión de producto. Añadir una comprobación de CI que falle si el APK destinado a release tiene `debuggable=true`, `BUILD_TYPE=debug` o un sujeto de certificado `Android Debug`.

### BLD-05-02 — La versión del paquete Android no coincide con la versión lógica de actualización

**Severidad: Alta**

La identidad instalada del APK es `versionCode=1` y `versionName=0.1.0`; el paquete y el `BuildConfig` lo confirman. En cambio, la actividad define `CURRENT_VERSION = "0.2.0"`, mientras que `distribution.json` usa `launcher_version: "0.2.0"` y `update.json` declara `version: "0.2.0"`. La lógica de comprobación compara el manifiesto remoto con `CURRENT_VERSION`, no con `BuildConfig.VERSION_NAME`.

**Evidencia.**

| Archivo o artefacto | Línea / dato verificado |
|---|---|
| `launcher/app/build.gradle` | 6–7: `versionCode 1; versionName '0.1.0'`. |
| `launcher/app/build/generated/source/buildConfig/debug/com/nosefc27/launcher/BuildConfig.java` | 7–11: `VERSION_CODE=1`, `VERSION_NAME="0.1.0"`. |
| `launcher/app/build/outputs/apk/debug/output-metadata.json` | `variantName="debug"`, `versionCode=1`, `versionName="0.1.0"`. |
| APK empaquetado (`aapt dump badging`) | `versionCode='1'`, `versionName='0.1.0'`, `sdkVersion:'23'`, `targetSdkVersion:'35'`. |
| `launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt` | 25: `CURRENT_VERSION = "0.2.0"`; 110–111: la comparación utiliza esta constante. |
| `launcher/distribution.json` | 2: `launcher_version` es `0.2.0`. |
| `launcher/update.json` | 2: `version` es `0.2.0`. |

**Impacto.** El usuario instala un paquete que Android identifica como 0.1.0, pero la aplicación se considera internamente 0.2.0. Esto invalida una fuente única de verdad para soporte, diagnóstico, control de roll-out y actualizaciones. En particular, el manifiesto remoto actual 0.2.0 no se ofrecerá a esa instalación porque la comparación interna la considera ya actualizada, aunque el paquete instalado declare 0.1.0. La inconsistencia también puede causar que se publique una actualización con un `versionCode` no monotónico, que Android rechazaría como downgrade.

**Corrección propuesta.** Centralizar versión y código de versión en Gradle, por ejemplo mediante propiedades versionadas, y derivar las referencias de ejecución desde `BuildConfig.VERSION_NAME` y `BuildConfig.VERSION_CODE`. El contenido de `update.json`, el nombre/tag del asset y `distribution.json` deben generarse o validarse contra esas mismas propiedades durante el pipeline de release. Establecer una regla: cada entrega pública incrementa estrictamente `versionCode`; `versionName`, manifest de actualización y nombre de release deben coincidir. Añadir una prueba o tarea de CI que lea el APK con `aapt`/`apkanalyzer` y falle si no coincide con los JSON publicados.

### BLD-05-03 — La receta de build no está autocontenida ni tiene controles completos de procedencia de dependencias

**Severidad: Media**

El proyecto fija las versiones directas relevantes —Android Gradle Plugin 8.6.1, Kotlin 2.0.21 y librerías AndroidX/Material— y existe un script de preparación que descarga Gradle 8.10.2 e instala Android API 35 y build-tools 35.0.0. Eso permitió reproducir el binario en esta auditoría. No obstante, `launcher/` no contiene `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties` ni el JAR del wrapper. Tampoco hay catálogo/lockfile de dependencias, `verification-metadata.xml`, hashes de las descargas de Gradle/SDK ni workflow CI. El script usa una ruta absoluta de estación de trabajo, descarga binarios sin validar hashes y no contiene el comando de ensamblado.

**Evidencia.**

| Archivo o artefacto | Línea / dato verificado |
|---|---|
| `launcher/build.gradle` | 1–4: AGP `8.6.1` y Kotlin `2.0.21` están fijados. |
| `launcher/settings.gradle` | 1–4: repositorios `google`, `mavenCentral` y `gradlePluginPortal`; módulo `:app`. |
| `launcher/app/build.gradle` | 15–18: versiones directas de cuatro dependencias fijadas. |
| `research/setup_android_build.sh` | 3, 6–8: Gradle 8.10.2 se descarga bajo `/home/ubuntu/work/android-toolchain`; 10–20: descarga command-line tools e instala API/build-tools 35.0.0. |
| Árbol de `launcher/` | No se hallaron `gradlew`, `gradlew.bat`, `gradle-wrapper.properties`, `gradle-wrapper.jar`, lockfiles, `verification-metadata.xml` ni `.github/workflows/`. |
| `.gitignore` | 6–7: `.gradle/` y `app/build/` se excluyen correctamente del control de versiones, pero no sustituyen una receta de build versionada. |

**Verificación de reproducibilidad realizada.** Se extrajo el contenido versionado mediante `git archive HEAD:launcher` a dos directorios limpios e independientes: `/home/ubuntu/jobs/541fe848ccae_a4/repro-1` y `…/repro-2`. Con Java 21, Gradle 8.10.2 y `ANDROID_HOME=/home/ubuntu/work/android-toolchain`, ambos ejecutaron con éxito `clean :app:assembleDebug`. Cada build generó el hash `bda30035886f11307ddad065ac43133e421486599ec2d402f80550a094c2c826`, que también coincide con el APK conservado. La prueba confirma reproducibilidad **en esta herramienta y con el caché/resolución disponible durante la auditoría**, no una garantía de reconstrucción independiente a largo plazo.

**Impacto.** Una estación nueva no puede invocar una versión de Gradle definida por el propio proyecto. Los cambios en disponibilidad, metadatos o binarios de repositorios podrían alterar o impedir una reconstrucción. La falta de verificación criptográfica de descargas/dependencias deja sin una comprobación versionada de que Gradle, SDK y artefactos Maven son los esperados. Sin CI, no hay control automático para evitar que un artefacto debug vuelva a sustituir al release.

**Corrección propuesta.** Incorporar Gradle Wrapper con distribución fijada y `distributionSha256Sum`, y documentar un único comando reproducible (`./gradlew --no-daemon :app:assembleRelease`). Versionar un catálogo de versiones o lockfiles de Gradle y activar dependency verification con `gradle/verification-metadata.xml`. En la receta de aprovisionamiento, fijar y verificar SHA-256 de los ZIP de Gradle y command-line tools; evitar rutas absolutas usando variables de entorno. Crear un workflow CI con JDK, Gradle/SDK y build-tools explícitos, que ejecute tests, genere release, verifique firma/SDK/versiones y publique hashes/SBOM. Mantener la prueba de hashes reproducidos como control adicional, no como sustituto de la firma de release.

### BLD-05-04 — Los recursos de aplicación no cubren los mínimos esperables para una distribución final

**Severidad: Media**

El árbol fuente de recursos propio contiene sólo dos archivos: el tema y la configuración de rutas del `FileProvider`. El manifiesto da un `android:label` literal (`FC 27`) y no declara `android:icon` ni `android:roundIcon`. No existen recursos propios de `mipmap`, `drawable`, `values/strings.xml`, `values/colors.xml`, `values-night`, iconos adaptativos o traducciones. Las numerosas entradas de recursos observadas dentro del APK proceden mayoritariamente de AppCompat/Material y no sustituyen recursos de marca ni de accesibilidad del producto.

**Evidencia.**

| Archivo o artefacto | Línea / dato verificado |
|---|---|
| `launcher/app/src/main/res/values/styles.xml` | 1–6: único recurso visual propio; define tema, fuente `sans` y color de acento. |
| `launcher/app/src/main/res/xml/file_paths.xml` | 1–3: segundo y único otro recurso propio. |
| `launcher/app/src/main/AndroidManifest.xml` | 4: etiqueta literal `FC 27`; 4–13: no declara `android:icon` ni `android:roundIcon`. |
| Inventario de `launcher/app/src/main/res/` | Dos archivos; no hay directorios fuente `mipmap*`, `drawable*` ni recursos de strings propios. |

**Impacto.** El launcher puede aparecer con iconografía genérica o no cumplir las expectativas de presentación del launcher del sistema. El texto literal en manifiesto no es localizable y no existe base propia de cadenas para textos de interfaz, accesibilidad o variantes de configuración. Esto reduce la calidad de producto y dificulta una publicación estable, aunque no impide técnicamente el build debug auditado.

**Corrección propuesta.** Añadir icono adaptativo y `roundIcon` en `mipmap-anydpi-v26` con capas foreground/background y fallbacks, declarar ambos en el manifiesto y definir etiquetas/cadenas en `values/strings.xml`. Externalizar colores y textos del código a recursos, incluir al menos variantes de tema coherentes si se ofrece modo oscuro y validar el APK en densidades y versiones Android representativas. La incorporación debe respetar los derechos de marca y recursos gráficos aplicables.

## Verificaciones sin hallazgo adverso

### Gradle, SDK y BuildConfig

La configuración declara `compileSdk 35`, `minSdk 23` y `targetSdk 35` en `launcher/app/build.gradle:6–7`. El APK empaquetado confirma `sdkVersion:'23'`, `targetSdkVersion:'35'` y `compileSdkVersion='35'`; el manifiesto fusionado también confirma `minSdkVersion="23"` y `targetSdkVersion="35"` en `launcher/app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml:7–9`. La compatibilidad real en dispositivos Android 6.0/API 23 y posteriores **no se verificó** porque no se ejecutaron pruebas en dispositivo o emulador.

`buildFeatures { buildConfig = true }` está activado en `launcher/app/build.gradle:8` y la clase generada contiene el `APPLICATION_ID` esperado, `com.nosefc27.launcher`, en la línea 8. El uso de `BuildConfig.APPLICATION_ID` para la autoridad de `FileProvider` es correcto, pero el BuildConfig inspeccionado es el de la variante debug; aún debe verificarse después de crear una variante release.

### AndroidManifest.xml

El manifiesto fuente solicita únicamente `INTERNET` y `REQUEST_INSTALL_PACKAGES` (`launcher/app/src/main/AndroidManifest.xml:2–3`) y declara una actividad launcher exportada explícitamente (`:5–9`), lo cual es coherente con una actividad principal en Android moderno. El manifiesto empaquetado confirma esos permisos. `REQUEST_INSTALL_PACKAGES` es una capacidad sensible y requiere justificación de distribución/política, pero su uso es coherente con la función de instalar el APK de juego que declara el código. La conformidad con políticas de Google Play o de cualquier tienda **no se verificó**.

El manifiesto fuente fija `android:allowBackup="false"` en la aplicación (`:4`), y el manifiesto fusionado conserva ese valor (`merged manifest:20–27`). No se observó `usesCleartextTraffic="true"`; los endpoints configurados en `MainActivity.kt:27`, `distribution.json:4–12` y `update.json:3` son HTTPS. No se realizó una prueba de red en dispositivo ni una evaluación de disponibilidad/autenticidad de esos endpoints durante esta auditoría.

### FileProvider

No se identificó una exposición excesiva en el `FileProvider`. El proveedor se declara con autoridad parametrizada `${applicationId}.files`, `android:exported="false"` y `android:grantUriPermissions="true"` en `launcher/app/src/main/AndroidManifest.xml:11–13`. En el APK resultante la autoridad se resuelve a `com.nosefc27.launcher.files`, y el manifiesto fusionado mantiene el proveedor no exportado con grants en `…/merged_manifests/debug/processDebugManifest/AndroidManifest.xml:38–46`.

Su archivo de rutas autoriza únicamente `<files-path name="downloads" path="downloads/" />` en `launcher/app/src/main/res/xml/file_paths.xml:1–3`. Esto coincide con el uso: `MainActivity.kt:60–61` descarga el APK a `File(filesDir, "downloads")`, y `:89–90` genera la URI con `${BuildConfig.APPLICATION_ID}.files` y otorga permiso temporal de lectura. Por tanto, autoridad, ruta y llamada están alineadas y no se observó una ruta raíz, externa o comodín que exponga otros archivos privados. La revocación explícita de URI al finalizar la instalación y el comportamiento frente a instaladores concretos **no se verificaron en dispositivo**.

## Limitaciones explícitas

Esta auditoría verificó archivos reales, manifiestos fusionados y un APK debug existente. No se verificaron una clave de firma de producción, su custodia, una variante release —porque no existe en la configuración inspeccionada—, Play App Signing, políticas de tiendas, instalación/actualización en un dispositivo físico, comportamiento de permisos especiales ni disponibilidad de los endpoints remotos. La igualdad de hashes fue verificada para el build debug con la cadena de herramientas disponible; no convierte por sí misma el proyecto en reproducible de forma independiente y permanente.

## Plan de corrección propuesto

1. **Bloquear la distribución actual.** Retirar o marcar inequívocamente el `FC27.apk` actual como debug/no distribuible hasta producir un release firmado por una clave de producción estable.
2. **Crear una release verificable.** Añadir configuración de `release`, firma mediante secretos, verificación de `apksigner`, y controles que rechacen certificado debug o `debuggable=true`.
3. **Unificar el versionado.** Elegir una única fuente Gradle para `versionCode`/`versionName`, usarla desde el código y generar/validar los JSON de actualización frente al APK final.
4. **Hacer el build autocontenido.** Añadir Gradle Wrapper con hash, verificación/bloqueo de dependencias, hashes de toolchain y CI que ejecute el pipeline de release.
5. **Completar recursos de producto.** Incorporar iconos adaptativos, strings y recursos de tema antes de una distribución final.

## Referencias de evidencia

La evidencia de esta auditoría procede exclusivamente de los archivos y artefactos locales citados mediante ruta y línea en cada hallazgo. No se usaron fuentes externas para sustentar conclusiones técnicas. La prueba de reproducibilidad se efectuó en copias efímeras fuera del repositorio del proyecto y no modificó el código auditado.
