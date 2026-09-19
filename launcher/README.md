# FC 27 Launcher

Launcher Android pequeño para preparar automáticamente APK, OBB y DATA. El diseño contempla descarga reanudable, verificación SHA-256, comprobación de espacio, extracción al almacenamiento privado de la aplicación y arranque del juego.

## Estado

Este directorio contiene el esqueleto inicial de desarrollo. Todavía no es un APK funcional ni modifica los binarios originales. Las URL de los paquetes se configurarán mediante un manifiesto de distribución firmado o servido desde un alojamiento público.

## Flujo objetivo

1. Mostrar el espacio requerido y la versión disponible.
2. Descargar OBB y DATA con reanudación.
3. Verificar tamaño y SHA-256.
4. Preparar recursos en almacenamiento privado.
5. Reanudar después de cerrar la aplicación o perder conexión.
6. Arrancar el motor únicamente cuando la preparación esté completa.

## Alojamiento

La Release actual de GitHub es privada y no es válida como origen para usuarios anónimos. Antes de una release pública del launcher habrá que elegir un almacenamiento público de distribución y colocar sus URL en el manifiesto. No se deben incrustar credenciales de GitHub en el APK.
