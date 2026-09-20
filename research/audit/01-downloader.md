# Auditoría de Downloader y flujo de distribución de FC27

**Fecha:** 2026-09-20  
**Alcance:** Se inspeccionaron los archivos reales `launcher/app/src/main/java/com/nosefc27/launcher/Downloader.kt` y `Distribution.kt`, y el tramo de `MainActivity.kt` que carga el manifiesto, descarga APK/OBB/DATA, recompone la DATA y la extrae. No se modificó código, no se eliminaron archivos y no se hicieron commits.

## Conclusión ejecutiva

**Resultado: riesgo alto.** El flujo transmite los archivos en streaming y calcula SHA-256 para cada recurso y para la DATA recompuesta. Esto evita cargar archivos de varios gigabytes en memoria y detecta corrupción una vez terminada la transferencia. Sin embargo, no es recuperable de forma fiable: un archivo ya completo, o un archivo corrupto que tenga la longitud final, provoca una solicitud `Range` que normalmente recibe `416 Range Not Satisfiable`. El flujo no verifica el hash antes de reanudar, no elimina ni sustituye el archivo bloqueado y no reintenta. Por ello, una ejecución interrumpida en un punto habitual puede dejar la preparación sin forma automática de continuar.

Además, el manifiesto que aporta simultáneamente las URL y los hashes se descarga desde una rama mutable de GitHub. El SHA-256 protege contra corrupción en tránsito respecto del manifiesto recibido, pero no autentica el contenido frente a una modificación autorizada o no autorizada de ese manifiesto. El espacio temporal tampoco se calcula ni se comprueba antes de descargar. Solo las tres partes de DATA se conservan dos veces durante el ensamblado, lo que requiere como mínimo **10.029.727.890 bytes (10,03 GB decimales; 9,34 GiB)** antes de sumar APK, OBB y datos extraídos.

| Área revisada | Estado | Observación principal |
|---|---|---|
| Reanudación HTTP | No fiable | Los archivos terminados o corruptos de tamaño final quedan bloqueados por `Range`/416. |
| Integridad SHA-256 | Parcial | Se valida cada descarga y la DATA unida, pero los hashes no están autenticados independientemente del manifiesto. |
| Reintentos | Ausentes | Una excepción de red, hash o HTTP requiere interacción manual y puede dejar un estado no recuperable. |
| Temporales y atomicidad | Insuficiente | Las descargas escriben directamente en los nombres finales; no existe archivo temporal, metadato de estado ni promoción atómica. |
| Memoria | Adecuada | La transferencia, hash, unión y extracción son por streaming; no se observó una carga completa de los ZIP en memoria. |
| Espacio en disco | Alto riesgo | No hay precomprobación y se duplican los datos durante el ensamblado. |

## Hallazgos

### DWN-01 — Un archivo completo o corrupto con longitud final deja la descarga sin recuperación

**Severidad: Alta.**

**Evidencia.** `Downloader.fetch` toma la longitud del destino existente (`Downloader.kt:12`) y, si es mayor que cero, envía `Range: bytes=<longitud>-` (`Downloader.kt:15`). Solo acepta respuestas 200 o 206 (`Downloader.kt:20-22`). El flujo siempre vuelve a invocar `download` para APK, OBB y cada parte de DATA, incluso si los ficheros ya existen (`MainActivity.kt:60-67`). Tras una descarga de parte correcta, `download` comprueba el SHA-256 (`MainActivity.kt:82-85`) y deja el archivo en su ruta final. En la siguiente ejecución, una petición que empieza exactamente al final de un recurso normalmente se responde con HTTP 416, código que el downloader rechaza antes de volver a calcular el hash local.

El mismo estado se produce si una descarga completa en longitud falla su SHA-256. El archivo corrupto se conserva (`MainActivity.kt:85` lanza sin limpieza), y la siguiente ejecución solicita el rango desde su longitud final. Las partes de DATA tienen explícitamente el tamaño esperado, por lo que este caso es alcanzable aun cuando el tamaño se valide (`Distribution.kt:5`, `MainActivity.kt:65-67`).

**Impacto.** El usuario puede completar APK, OBB o una parte, fallar en un paso posterior y no poder reanudar al pulsar de nuevo el botón. También puede quedar bloqueado después de una transferencia corrupta que conserve el número correcto de bytes. Esto contradice la propiedad de reanudación anunciada y afecta especialmente a recursos de varios gigabytes.

**Corrección propuesta.** Usar una ruta temporal por recurso, por ejemplo `<nombre>.part`, y mantener un estado explícito de recurso validado. Antes de emitir `Range`, verificar el hash local cuando el tamaño conocido coincide con el esperado; si es válido, devolver éxito sin descargar. Si el servidor devuelve 416, aceptar el archivo únicamente si su hash coincide; en caso contrario, borrar el temporal corrupto y reiniciar desde cero. Después de hash y tamaño correctos, promover el temporal a la ruta final mediante renombrado atómico en el mismo directorio.

### DWN-02 — La validación SHA-256 no autentica un manifiesto mutable

**Severidad: Alta.**

**Evidencia.** `Distribution.fromJson` toma desde el mismo texto JSON tanto las URL como los valores SHA-256 (`Distribution.kt:8-12`). El manifiesto se descarga desde `raw.githubusercontent.com/.../master/launcher/distribution.json` (`MainActivity.kt:24-27`, `MainActivity.kt:46` y `MainActivity.kt:59`). Después, el flujo compara cada descarga contra el hash que venía en ese manifiesto (`MainActivity.kt:82-85`), y compara el ZIP DATA ensamblado contra `data_sha256` del mismo origen (`MainActivity.kt:69-74`). No existe firma de manifiesto, clave pública embebida, versión inmutable ni anclaje a un commit o release.

**Impacto.** HTTPS protege el canal frente a ataques de red ordinarios, pero un actor que pueda modificar la rama o el origen del manifiesto puede cambiar a la vez URL y hash. En ese escenario, el launcher descargará y aceptará el contenido nuevo. La comprobación actual detecta corrupción accidental respecto del manifiesto recibido, no establece una cadena de confianza independiente para APK, OBB o DATA.

**Corrección propuesta.** Publicar un manifiesto versionado e inmutable y firmarlo fuera del repositorio mutable. El launcher debe incluir una clave pública de verificación y rechazar manifiestos cuya firma, esquema, versión o expiración no sean válidos. Las URL deberían estar ligadas a assets/versiones inmutables. La firma debe cubrir todos los campos sensibles: URL, SHA-256, tamaño, identificador de paquete, versión y lista ordenada de partes.

### DWN-03 — La reanudación acepta cualquier respuesta 206 sin comprobar el rango devuelto

**Severidad: Media.**

**Evidencia.** Cuando existe un destino previo, el código posiciona `RandomAccessFile` en `existing` (`Downloader.kt:12-16`, `Downloader.kt:24-25`) y acepta cualquier respuesta HTTP 206 (`Downloader.kt:20-22`). No lee ni valida los encabezados `Content-Range`, `Content-Length`, `ETag` o `Last-Modified`. Tampoco asocia el fragmento local con una versión concreta del recurso remoto. Para APK y OBB, `MainActivity.kt:63-64` pasa `null` como `expectedBytes`, por lo que `Downloader.kt:35` no realiza validación de tamaño; las partes DATA sí aportan tamaños desde el manifiesto (`MainActivity.kt:65-67`).

**Impacto.** Si un proxy, CDN u origen devuelve 206 con un inicio distinto del solicitado, o cambia el objeto entre una transferencia parcial y su reanudación, el downloader escribe los bytes recibidos desde el desplazamiento local esperado. El SHA-256 final normalmente detectará el resultado corrupto, pero solo después de transferir el resto del archivo y, por DWN-01, puede dejarlo en un estado no recuperable. La falta de tamaños declarados para APK y OBB también impide una detección temprana de respuestas truncadas o de progreso fiable cuando falta `Content-Length`.

**Corrección propuesta.** Para toda reanudación, exigir `206`, analizar `Content-Range` y comprobar que su byte inicial coincide exactamente con `existing`; comprobar además el total cuando esté disponible. Guardar ETag o Last-Modified junto al temporal y reiniciar si el validador remoto cambia. Añadir `size_bytes` para APK y OBB al contrato `Distribution`, validar tamaño antes del hash y no aceptar contenido parcial ambiguo. Si el servidor ignora `Range` y devuelve 200, truncar o recrear el temporal de forma comprobada antes de escribir desde cero.

### DWN-04 — No hay política de reintentos ni limpieza transaccional al fallar

**Severidad: Media.**

**Evidencia.** `Downloader.fetch` realiza una única apertura de conexión con tiempos de espera de 20 segundos para conexión y 60 segundos para lectura (`Downloader.kt:13-16`). No hay bucle de reintentos, espera incremental, clasificación de errores recuperables ni reconexión. El `disconnect()` aparece solo después de la copia, la comprobación de tamaño y los cierres normales (`Downloader.kt:24-35`), por lo que una excepción previa salta esa llamada. En el nivel superior, cualquier excepción solo actualiza el texto de estado y reactiva el botón (`MainActivity.kt:57-79`); no elimina el archivo que no pasó hash ni registra el estado de la operación.

**Impacto.** Una pausa de red superior a 60 segundos, un 5xx transitorio, una redirección fallida o una desconexión obliga al usuario a reiniciar manualmente. Aunque una descarga parcialmente escrita puede continuar en el caso ideal, no hay garantía de que se haga y los casos de longitud final incorrecta quedan bloqueados. La ausencia de `finally` para desconectar puede prolongar la vida de recursos de red en rutas de error.

**Corrección propuesta.** Implementar reintentos acotados para errores transitorios de red y HTTP 408, 429 y 5xx, con backoff exponencial y respeto de `Retry-After`. Mantener el temporal para reanudar, pero borrar o invalidar explícitamente todo archivo que falle su hash o su tamaño. Colocar el cierre/desconexión en `finally` o usar una abstracción HTTP con cierre estructurado. La política debe ser cancelable con el ciclo de vida de Android y debe informar al usuario del intento y del motivo definitivo de fallo.

### DWN-05 — El ensamblado duplica al menos 10,03 GB y no existe comprobación de espacio

**Severidad: Alta.**

**Evidencia.** El manifiesto real declara partes DATA de 1.887.436.800, 1.887.436.800 y 1.239.990.345 bytes, cuya suma es 5.014.863.945 bytes (`launcher/distribution.json:data_parts`). Estas partes se descargan a `filesDir/downloads/data-<índice>.part` (`MainActivity.kt:60-67`) y se conservan. Después se crea otro archivo completo, `Fifa16ModFC27.assembled.zip`, y se copia secuencialmente cada parte sin eliminar las originales (`MainActivity.kt:69-74`). Finalmente se extrae tanto OBB como DATA a `filesDir/prepared` (`MainActivity.kt:75-76`). La pantalla solo comunica una necesidad aproximada de 6,2 GB (`MainActivity.kt:49`) y no hay llamada de comprobación de espacio en este flujo.

**Impacto.** Solo las partes y el ZIP DATA ensamblado ocupan 10.029.727.890 bytes antes de incluir APK, OBB, los ZIP y los archivos descomprimidos. Si el almacenamiento se agota durante descarga o extracción, quedan artefactos parciales y la corrección manual puede requerir borrar datos de la aplicación. La estimación de 6,2 GB no describe el máximo de espacio de trabajo que exige la implementación.

**Corrección propuesta.** Antes de iniciar, calcular el máximo de espacio de trabajo a partir de tamaños comprimidos y una estimación o metadatos del contenido extraído; comprobarlo con `StatFs` y reservar margen. Evitar el ZIP DATA ensamblado cuando sea posible: alimentar la extracción mediante un flujo concatenado de partes verificadas, o eliminar cada parte tras una promoción/consumo seguro si el formato y la estrategia de recuperación lo permiten. Limpiar los temporales y el ZIP ensamblado tras una preparación verificada, y presentar al usuario el requisito de espacio máximo, no solo el tamaño final aproximado.

### DWN-06 — Los nombres finales se usan como temporales y la sustitución no es atómica

**Severidad: Media.**

**Evidencia.** APK, OBB y las partes se escriben directamente en sus nombres de destino (`MainActivity.kt:61-67`; `Downloader.kt:10-12`, `Downloader.kt:24-31`). Al recibir HTTP 200 para una reanudación, el código llama a `target.delete()` y no comprueba su resultado (`Downloader.kt:17-19`); a continuación escribe con `RandomAccessFile` sin truncar expresamente el archivo (`Downloader.kt:24-25`). El archivo ensamblado también se borra sin comprobar el resultado y se abre con modo append (`MainActivity.kt:69-72`). No hay marca que distinga una descarga parcial, validada, ensamblada o extraída por completo.

**Impacto.** Un fallo de borrado puede conservar bytes residuales al reescribir un archivo más corto, o hacer que el ensamblado se anexe sobre contenido previo. Los SHA-256 suelen detectar la corrupción, pero el estado persistente resultante no se limpia y interactúa con el bloqueo descrito en DWN-01. Además, una extracción interrumpida puede dejar `prepared/obb` o `prepared/data` parcialmente actualizados; una ejecución posterior no elimina entradas antiguas que ya no estén en el ZIP.

**Corrección propuesta.** Crear destinos temporales exclusivos, abrirlos con truncado explícito al reiniciar y comprobar todos los resultados de borrado, creación, cierre y movimiento. Una vez validados tamaño y SHA-256, usar movimiento atómico a un nombre final y guardar un manifiesto local de preparación completada. Extraer en un directorio de staging nuevo y sustituir el directorio preparado solo después de una extracción correcta; así se evita mezclar restos de una ejecución anterior.

## Observación favorable sobre memoria

No se identificó una carga de archivos grandes completa en memoria. La transferencia usa un búfer de 1 MiB (`Downloader.kt:27-31`). El cálculo SHA-256 usa un `BufferedInputStream` de 1 MiB y otro búfer de 1 MiB (`Downloader.kt:38-43`). La unión de partes emplea `input.copyTo(output)` de una parte por vez (`MainActivity.kt:71-72`) y la extracción usa `ZipInputStream` de manera secuencial (`MainActivity.kt:93-105`). Por tanto, el riesgo dominante es **almacenamiento temporal**, no memoria del proceso.

Esta conclusión no establece un límite absoluto de memoria del proceso: la pila de red, la biblioteca ZIP y Android añaden consumo no visible en estas líneas. Aun así, el código auditado no materializa en RAM APK, OBB ni el ZIP DATA completo.

## Límites de verificación

La auditoría fue estática sobre el árbol local indicado. No se ejecutó la aplicación en Android, no se descargaron los assets de varios gigabytes y no se hizo una prueba HTTP contra los orígenes reales. Por ello, no se verificó empíricamente si el CDN actual responde 206 con `Content-Range` correcto, qué hace exactamente ante una solicitud al final del archivo, ni si las redirecciones preservan `Range`. El hallazgo DWN-01 se basa en el comportamiento que el código impone ante la respuesta HTTP 416 y en la semántica esperada de una solicitud de rango al final de un recurso.

Tampoco es posible calcular el pico exacto de espacio desde el manifiesto actual, porque APK y OBB no declaran `size_bytes` y no se dispone aquí del tamaño descomprimido de ambos ZIP. El mínimo de 10,03 GB expuesto en DWN-05 corresponde exclusivamente a conservar las tres partes DATA y su copia ensamblada. No se auditó el flujo separado de actualización del launcher ni se evaluó la autorización para redistribuir los assets.

## Referencias

[1]: https://github.com/ayoubnoob543-lab/nose-fc-14-fc-27/blob/master/launcher/app/src/main/java/com/nosefc27/launcher/Downloader.kt "Downloader.kt del launcher FC27"
[2]: https://github.com/ayoubnoob543-lab/nose-fc-14-fc-27/blob/master/launcher/app/src/main/java/com/nosefc27/launcher/Distribution.kt "Distribution.kt del launcher FC27"
[3]: https://github.com/ayoubnoob543-lab/nose-fc-14-fc-27/blob/master/launcher/app/src/main/java/com/nosefc27/launcher/MainActivity.kt "MainActivity.kt del launcher FC27"
[4]: https://github.com/ayoubnoob543-lab/nose-fc-14-fc-27/blob/master/launcher/distribution.json "Manifiesto de distribución FC27"
