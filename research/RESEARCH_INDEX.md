# Índice de investigación FC27 / FIFA 14

Este repositorio conserva los informes, inventarios, hashes, scripts y auditorías realizados durante el análisis. Los archivos APK, OBB, DATA, builds y extracciones binarias permanecen fuera del historial Git por tamaño, seguridad y derechos de redistribución.

## Contenido

- `INFORME_FIFA14_1.3.0.3.md`: análisis de la base FIFA 14 Android.
- `INFORME_CANDIDATOS_OBB_FC27_2026-09.md`: comparación de fuentes comunitarias.
- `INFORME_OBB_RECIBIDO_FC27.md`: estructura BIG y recursos del OBB recibido.
- `INFORME_DATA_FC27.md`: inventario de DATA y rutas visuales.
- `AUDITORIA_TECNICA_CONSOLIDADA.md`: auditoría del launcher y orden de correcciones.
- `audit/`: auditorías independientes de downloader, almacenamiento, actualización, seguridad, build y UX.
- `fc27-obb-upload/`: inventarios BIG4, rutas de menú, HUD, marcador, iconos y texturas.
- `fc27-data-upload/`: rutas de menús, fondos, HUD, caras, kits, jugadores, plantillas y texturas.
- `security-scan-2026-09-20/`: análisis estático, permisos y manifiesto del APK.
- `source/`: capturas y referencias de fuentes utilizadas.
- Scripts `.sh` y `.py`: procedimientos reproducibles de descarga e inventario.

## Exclusiones intencionadas

`originals/`, `release-assets/`, `launcher/app/build/`, `launcher/.gradle/` y las carpetas de extracción contienen binarios grandes, cachés o copias de terceros. No se suben al repositorio. Sus tamaños, hashes y estructuras sí quedan documentados en los informes y manifiestos versionados.
