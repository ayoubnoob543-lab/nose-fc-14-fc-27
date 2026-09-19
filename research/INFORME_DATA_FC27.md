# Informe de DATA — FIFA 16 MOD FC 27 de MAHGAMES

## Validación

La DATA recibida se conservó como copia original sin modificar y pasó la comprobación completa del ZIP.

| Campo | Resultado |
|---|---|
| Archivo | `Fifa16ModFC27.zip` |
| Tamaño | 5.014.863.945 bytes (~4,67 GiB) |
| SHA-256 | `739a822f51f774eba37bd12b2fe27aea0742ea0c451b42fe010df4e7bbb479b7` |
| Formato | ZIP almacenado sin compresión |
| Miembros del ZIP | 35.402 entradas totales; 34.530 archivos |
| Test de integridad | Correcto; `No errors detected in compressed data` |
| Ruta raíz | `com.ea.gp.fifaworld/` |

## Compatibilidad

La ruta raíz de la DATA coincide exactamente con el package ID del APK y con la carpeta del OBB: `com.ea.gp.fifaworld`. Esto confirma compatibilidad de identidad entre los tres componentes. La DATA no contiene archivos `.big` propios; su contenido está distribuido principalmente como PNG, binarios, bases de datos, recursos UX y assets de escena.

El APK identificado es `com.ea.gp.fifaworld`, version code 26, version name 3.2.113645. El OBB contiene `main.13.com.ea.gp.fifaworld.obb` y `patch.26.com.ea.gp.fifaworld.obb`. La combinación de package ID, nombres de expansión y ruta raíz de DATA es coherente.

## Estructura funcional

| Área | Archivos/rutas detectadas |
|---|---:|
| `assets/common/` | 14.854 |
| `data/sceneassets/` | 11.556 |
| `data/ux/` | 3.932 |
| `assets/hud/` | 1.452 |
| `assets/MainMenu/` | 671 |
| `assets/SMR21/` | 461 |
| `assets/PAHAD/` | 1.253 |
| `assets/squad/` | 46 |
| `assets/Tactical/` | 22 |
| `assets/gamecontrols/` | 19 |

La clasificación por nombre detectó aproximadamente 1.069 rutas relacionadas con el menú, 287 con fondos, 520 con caras/heads, 12.093 con kits o uniformes, 763 con plantillas/squads, 2.152 con HUD/marcador y 18.575 texturas. Son conteos de candidatos por nombre y no sustituyen una inspección visual de cada recurso.

## Recursos de menú y fondos

La DATA contiene rutas directas que facilitan una futura sustitución visual:

```text
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_0.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_13.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_14.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_16.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_19.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_2216.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_2218.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_2222.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_2235.png
com.ea.gp.fifaworld/assets/MainMenu/Background/Bg_2236.png
com.ea.gp.fifaworld/assets/MainMenu/BackgroundOffice/l0.png
com.ea.gp.fifaworld/assets/MainMenu/BackgroundOffice/l13.png
com.ea.gp.fifaworld/assets/MainMenu/BgLeague/bl13.png
com.ea.gp.fifaworld/assets/MainMenu/BgLeague/bl16.png
com.ea.gp.fifaworld/assets/MainMenu/BgLeague/bl19.png
```

La carpeta `assets/MainMenu/Logo/` contiene logos para goleadores y clasificación. El archivo `product.ini` demuestra que la DATA también incluye ajustes de cámara y gameplay del mod, no solamente imágenes.

## Caras, jugadores, kits y plantillas

La DATA contiene recursos numerados y organizados para personalización masiva:

```text
com.ea.gp.fifaworld/assets/common/...
com.ea.gp.fifaworld/assets/sceneassets/...
com.ea.gp.fifaworld/assets/squad/...
com.ea.gp.fifaworld/assets/PAHAD/...
com.ea.gp.fifaworld/assets/Tactical/...
```

La presencia de más de 12.000 candidatos con `kit` o `uniform`, más de 500 candidatos con `face`/`head` y decenas de rutas `squad`/`roster` indica que será viable estudiar fichajes, equipaciones y caras como fases independientes. Antes de reemplazar recursos habrá que comprobar sus dimensiones, formato, compresión, identificadores y referencias cruzadas.

## Implicación para el APK único

El paquete único tendría que incorporar el APK, el OBB y esta DATA. Con el tamaño actual, el contenido lógico mínimo es aproximadamente:

```text
APK                 ~29 MiB
OBB extraído        ~1,49 GiB
DATA                ~4,67 GiB
--------------------------------
Total mínimo        ~6,2 GiB
```

El ZIP de DATA pesa 5.014.863.945 bytes porque está almacenado sin compresión; el tamaño final del APK dependerá de si los recursos se integran comprimidos, sin compresión o en un contenedor propio. GitHub no admite este ZIP como un único asset de Release de tamaño ilimitado, por lo que para conservarlo allí se debe dividir en partes menores de 2 GB y recomponerlo con hashes.

Un APK único funcional sigue necesitando un loader modificado. La coincidencia de rutas confirma que el conjunto actual es compatible, pero no demuestra todavía que el motor pueda leer la DATA desde `assets/` o desde almacenamiento privado. Ese comportamiento deberá probarse en una copia de desarrollo.

## Archivos de inventario

- `data.sha256`
- `data-members.txt`
- `data-top-list.txt`
- `data-top-level.txt`
- `data-content-candidates.txt`
- `directory-counts.tsv`
- `main_menu-paths.txt`
- `background-paths.txt`
- `face-paths.txt`
- `kit-paths.txt`
- `roster-paths.txt`
- `hud-paths.txt`
- `texture-paths.txt`

No se modificó el ZIP original.
