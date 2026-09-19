# Plan técnico — APK único descargable y mejoras futuras

## Objetivo

El objetivo del proyecto será convertir la base `com.ea.gp.fifaworld` en una distribución de un único APK descargable que prepare automáticamente todos los recursos necesarios. El APK final deberá incorporar o preparar el contenido del OBB y de la DATA sin que el usuario tenga que copiar manualmente carpetas en `Android/obb` o `Android/data`.

Por ahora no se modifica ningún APK, OBB o DATA original. Los binarios recibidos se conservan como copias de referencia y cualquier trabajo futuro se hará sobre copias de análisis.

## Situación actual

El repositorio privado ya contiene el APK, el OBB y los informes. El APK relacionado tiene package ID `com.ea.gp.fifaworld`, version code `26`, version name `3.2.113645`, min SDK 19 y target SDK 27. El OBB contiene `main.13.com.ea.gp.fifaworld.obb` y `patch.26.com.ea.gp.fifaworld.obb`, ambos contenedores BIG4.

La DATA `Fifa16ModFC27.zip` todavía debe recibirse. Sin ella no se puede confirmar la estructura final, sus rutas, sus hashes ni la compatibilidad completa con el APK y el OBB.

## Arquitectura objetivo

La primera opción a estudiar será un APK con un bootstrapper propio. En el primer inicio, el bootstrapper comprobará hashes, reservará espacio, preparará los recursos en almacenamiento privado de la aplicación y generará un manifiesto de rutas para el motor. Si el motor no puede leer directamente desde assets comprimidos, se implementará extracción controlada a la ruta privada de la aplicación. El APK deberá poder reanudar una preparación interrumpida y verificar el espacio libre antes de comenzar.

La segunda opción, si el motor exige rutas externas o acceso directo a archivos grandes, será un APK instalador que prepare automáticamente los recursos y arranque el motor desde una ubicación compatible. El usuario seguiría descargando un solo archivo, aunque internamente el sistema podría usar almacenamiento privado de la aplicación en vez de `Android/data` público.

## Fases de trabajo

### Fase 1 — DATA y compatibilidad

Recibir `Fifa16ModFC27.zip`, conservar el original, calcular SHA-256, comprobar el ZIP, inventariar rutas y comparar su package ID, carpetas y nombres con `com.ea.gp.fifaworld`.

### Fase 2 — Mapa de carga

Analizar el manifiesto, las clases de descarga/expansión, la librería nativa y las cadenas de rutas. Identificar si el motor abre OBB por ruta fija, si acepta un directorio alternativo y qué parte de la DATA es requerida en el arranque.

### Fase 3 — Prototipo reversible

Crear una copia de trabajo del APK y un loader de prueba. No se alterarán los originales. El prototipo tendrá validación de hashes, registro de errores, comprobación de espacio y extracción reanudable. Se probará primero con un subconjunto pequeño de recursos.

### Fase 4 — APK único de prueba

Integrar los recursos de prueba, ajustar el loader y construir una variante firmada de desarrollo. Se comprobará instalación limpia, primer arranque, reinicio después de interrupción, falta de espacio y Android 14/15.

### Fase 5 — Mejoras de contenido

Cuando el APK base sea estable se podrán añadir, en copias de trabajo y con recursos autorizados o proporcionados por el usuario:

- Actualización de plantillas y fichajes.
- Mejoras de caras y head assets.
- Versiones visuales alternativas.
- Nuevos fondos, menús, iconos, HUD y marcador.
- Ajustes de cámaras, kits y presentación.
- Mejoras de rendimiento y reducción de tiempos de carga.

Cada mejora tendrá su propio manifiesto, hash, changelog y versión de release para poder revertirla.

## Estimación de tamaño

Con el OBB actual y la DATA anunciada, el APK único podría superar aproximadamente 6,3 GB. Si se incorpora material adicional como un Facepack grande, puede superar 9,5 GB. La instalación o extracción puede requerir 12–15 GB de espacio temporal. El tamaño real solo se calculará después de recibir y analizar la DATA.

## Limitaciones importantes

Un APK grande no garantiza funcionalidad: el motor nativo debe localizar y abrir los recursos. Android 14/15 también impone restricciones sobre rutas públicas `Android/data` y `Android/obb`. Por eso se priorizará almacenamiento privado de la aplicación o lectura desde un contenedor propio cuando sea compatible.

No se modificará ni redistribuirá ningún archivo original en esta fase. El repositorio mantendrá los originales separados de las copias de trabajo. No se publicará una release final hasta validar arranque, integridad, compatibilidad y procedencia de los recursos.

## Próximo paso

Subir `Fifa16ModFC27.zip`. Después se hará el análisis de DATA y se cerrará el diseño del loader antes de tocar cualquier binario.
