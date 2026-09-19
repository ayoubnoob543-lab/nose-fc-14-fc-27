# FC 27 Launcher

Launcher Android pequeño para preparar automáticamente APK, OBB y DATA. El diseño contempla descarga reanudable, verificación SHA-256, comprobación de espacio, extracción al almacenamiento privado de la aplicación y arranque del juego.

## Estado

El prototipo incluye ahora la comprobación de actualizaciones mediante un manifiesto JSON HTTPS. Si detecta una versión superior, muestra una pantalla de actualización con versión, notas, tamaño y botones **Actualizar** / **Ahora no**. El APK de actualización se abre mediante el instalador seguro de Android; no se intenta instalar silenciosamente ni se incluyen credenciales.

La URL provisional es `https://example.com/fc27/update.json` y debe sustituirse por la URL real del CDN antes de publicar. Todavía no es un APK de distribución final ni modifica los binarios originales.

## Manifiesto de actualización

El formato está documentado en `update.json.example`:

```json
{
  "version": "0.2.0",
  "apk_url": "https://cdn.example.com/fc27/FC27-0.2.0.apk",
  "sha256": "...",
  "size_bytes": 12345678,
  "mandatory": false,
  "notes": "Cambios de esta versión"
}
```

El manifiesto debe servirse por HTTPS y el APK debe publicarse con un hash SHA-256 verificable. Para una versión obligatoria se usa `mandatory: true`; para una actualización opcional, `false`.

## Flujo objetivo

1. Mostrar el espacio requerido y la versión disponible.
2. Comprobar el manifiesto remoto al abrir FC 27.
3. Preguntar si se quiere actualizar cuando existe una versión más reciente.
4. Descargar OBB y DATA con reanudación.
5. Verificar tamaño y SHA-256.
6. Preparar recursos en almacenamiento privado.
7. Reanudar después de cerrar la aplicación o perder conexión.
8. Arrancar el motor únicamente cuando la preparación esté completa.

## Alojamiento

La Release actual de GitHub es privada y no es válida como origen para usuarios anónimos. Antes de una release pública del launcher habrá que elegir un almacenamiento público de distribución y colocar sus URL en el manifiesto. No se deben incrustar credenciales de GitHub en el APK. Los archivos del juego solo deben publicarse si existe autorización para redistribuirlos.
