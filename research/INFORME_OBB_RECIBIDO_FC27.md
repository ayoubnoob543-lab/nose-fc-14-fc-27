# Resultado provisional — OBB FIFA 16 MOD FC 27 de MAHGAMES

## Estado

Se recibió `obb.zip` como archivo adjunto y se conservó una copia original sin modificar. El archivo es válido y contiene dos OBB internos. La DATA todavía no ha sido recibida, por lo que la compatibilidad del conjunto completo APK+OBB+DATA queda pendiente.

## OBB externo

| Campo | Resultado |
|---|---|
| Nombre recibido | `obb.zip` |
| Tamaño | 1.425.685.730 bytes |
| Tipo | ZIP almacenado sin compresión para los miembros principales |
| SHA-256 | `8c1e18a31e799a45c8e45f9e41a8662f7360c8e732d80aef8661d10d966e7b06` |
| Test ZIP | Correcto; `No errors detected in compressed data` |
| Carpeta | `obb/com.ea.gp.fifaworld/` |

## OBB internos

| Archivo | Tamaño | Formato | Miembros BIG4 |
|---|---:|---|---:|
| `main.13.com.ea.gp.fifaworld.obb` | 1.495.716.117 bytes | `BIG4` | 18.768 |
| `patch.26.com.ea.gp.fifaworld.obb` | 102.869.675 bytes | `BIG4` | 2.562 |
| **Total sin compresión** | **1.598.585.792 bytes** | | **21.330** |

El nombre de carpeta y el package ID coinciden con el APK descargado anteriormente: `com.ea.gp.fifaworld`.

## APK relacionado ya descargado

| Campo | Resultado |
|---|---|
| Nombre local | `EA_SPORTS_FC_MOBILE_BETA.apk` |
| Tamaño | 30.670.718 bytes |
| SHA-256 | `1bcea18ab5c01f5ce7e6c4cdc132a591194fe80b07d22548d66c6f74fce34b01` |
| Package ID | `com.ea.gp.fifaworld` |
| Version code | `26` |
| Version name | `3.2.113645` |
| Min SDK | 19 |
| Target SDK | 27 |
| Arquitectura observada | `armeabi-v7a` |

La coincidencia del package ID y la presencia de `main.13`/`patch.26`, junto con el APK de versión code 26, son evidencia fuerte de compatibilidad de identidad. La compatibilidad de DATA todavía no puede declararse hasta recibir `Fifa16ModFC27.zip` y revisar sus rutas y metadatos.

## Recursos visuales localizados

Los dos BIG4 fueron inventariados sin extraer masivamente sus 21.330 miembros. El inventario detecta aproximadamente 331 layouts, 315 rutas HUD, 110 fondos, 24 iconos y 21 botones mediante clasificación por nombre; son conteos de rutas candidatas, no una clasificación semántica perfecta.

### Menús e interfaz

Rutas concretas relevantes:

```text
data\ux\flows\ingameflow\gamemenus\GameMenu.dd
data\ux\flows\ingameflow\gamemenus\GameMenu.lua
data\ux\flows\ingameflow\gamemenus\GameMenuRows.layout
data\ux\flows\ingameflow\gamemenus\PauseMenu.layout
data\ux\flows\ingameflow\gamemenus\PauseMenu.vvm
data\ux\flows\ingameflow\gamemenus\PostmatchMenu.layout
data\ux\flows\ingameflow\gamemenus\SkillGameMenu.lua
data\ux\flows\ingameflow\gamemenus\SkillPauseMenu.layout
data\ux\flows\mainflow\gamemodes\fut\home\FUTHomeHub.layout
data\ux\flows\mainflow\gamemodes\fut\home\FUTHomeHub.lua
```

También aparecen `data/camera/FEMenuCamValues_*.dat`, que controlan cámaras de front-end y menús, y `data/ux/views/backgrounds/StageBackground.swf` junto con `logoBGAnimation.swf`, que son candidatos directos para fondos y animaciones de interfaz.

### Fondos

Se encontraron, entre otros:

```text
splash.png
splash_16by10.png
splash_widescreen.png
data\ux\views\backgrounds\StageBackground.swf
data\ux\views\backgrounds\logoBGAnimation.swf
assets\components\item\background\player\...\*.png
```

Los cientos de `assets/components/item/background/player/.../*.png` son fondos de elementos/player cards y no necesariamente el fondo principal del menú.

### Iconos y botones

Rutas exactas:

```text
assets\common\button\popup\popupbuttonhighlightblue.png
assets\common\button\popup\popupbuttonhighlightdark.png
assets\common\button\popup\popupbuttonidleblue.png
assets\common\button\popup\popupbuttonidledark.png
assets\common\button\ticker\footerbluebuttonidle_angle_left.png
assets\common\button\ticker\footerbluebuttonidle_angle_right.png
assets\common\icons\footer\icon-exit.png
assets\common\icons\footer\icon-retry.png
assets\common\icons\widget\icon_back.png
assets\common\icons\widget\icon_ball.png
assets\common\icons\widget\icon_reward.png
data\ux\assetsLicensedTeamCrests.big
data\ux\assets\common\button\glint\genericButton_glint.swf
data\ux\assets\common\button\glint\genericIcon_glint.swf
```

### HUD

El OBB contiene un conjunto amplio y explícito de HUDs bajo:

```text
data\ux\flows\ingameflow\huds\
```

Ejemplos identificados:

```text
data\ux\flows\ingameflow\huds\gameevents\GameEvents.layout
data\ux\flows\ingameflow\huds\matchinfo\MatchInfo.layout
data\ux\flows\ingameflow\huds\matchinfonpc\MatchInfoNPC.layout
data\ux\flows\ingameflow\huds\mentality\Mentality.layout
data\ux\flows\ingameflow\huds\playernib\PlayerNIBHome.layout
data\ux\flows\ingameflow\huds\attackdefensehud\attack\Gamepad2Buttons.layout
data\ux\flows\ingameflow\huds\attackdefensehud\defense\Gamepad2Buttons.layout
```

Cada grupo suele incluir archivos `.dd`, `.layout`, `.lua`, `.lreq` y `.vvm`, por lo que no se trata solamente de imágenes: también contiene layouts y lógica de presentación.

### Marcador y reloj

Se localizaron rutas específicas:

```text
data\ux\flows\ingameflow\huds\scoreclock\ScoreClock.dd
data\ux\flows\ingameflow\huds\scoreclock\ScoreClock.layout
data\ux\flows\ingameflow\huds\scoreclock\ScoreClock.lua
data\ux\flows\ingameflow\huds\scoreboardsubstitution\ScoreboardSubstitutions.layout
data\ux\flows\ingameflow\huds\scoreboardsubstitution\ScoreboardSubstitutions.lua
data\ux\flows\ingameflow\halftime.nav
data\ux\flows\ingameflow\othalftime.nav
```

Esto confirma que el marcador/reloj está separado conceptualmente del resto del HUD y tiene layouts y lógica específicos.

## Viabilidad de un único APK

### Posible técnicamente, pero no como simple renombrado

El APK ya incluye clases como `SampleDownloaderService`, lo que indica que el flujo original contempla archivos de expansión. Para convertirlo en un único APK habría que modificar el flujo de carga y el motor para que los recursos se obtengan desde el propio APK o desde almacenamiento privado de la aplicación.

Hay tres opciones:

| Opción | Resultado | Riesgo técnico |
|---|---|---|
| APK que contiene el OBB/DATA como `assets/` y los extrae al iniciar | Instalación de un único APK; requiere copiar aproximadamente 1,6 GB de OBB descomprimido y hasta 4,76 GB de DATA | Alto: requiere espacio temporal, extracción larga y redirigir el motor |
| APK auto-instalador que copia OBB/DATA a sus ubicaciones | El usuario instala un APK y pulsa iniciar; no copia manualmente | Medio-alto: Android 14/15 restringe el acceso general a `Android/data` y `Android/obb`; el APK debe trabajar con rutas propias o permisos adecuados |
| APK/AAB con recursos grandes internos y motor parcheado | Un paquete lógico; puede evitar `Android/data` | Muy alto: cambios en loader/nativo, límites de instalación, tiempos y almacenamiento |

### Estimación de espacio

Con los datos disponibles, el mínimo de recursos de expansión es:

```text
APK                         30.670.718 bytes       ~29 MiB
OBB interno descomprimido   1.598.585.792 bytes    ~1,49 GiB
DATA anunciada               ~4,76 GB               pendiente de verificar
--------------------------------------------------------------
Total lógico mínimo          ~6,28 GB               sin temporales ni Facepack
```

Un APK final que incluyera todo podría ocupar aproximadamente **6,3–6,8 GB**, dependiendo de la compresión y de si también se incorpora el Facepack de 3,23 GB. Durante la instalación/extracción podría necesitarse bastante más espacio temporal, potencialmente **12–15 GB**.

Android permite instalar APKs grandes fuera de Google Play en ciertos escenarios, pero el tamaño no es el único problema: el motor debe encontrar los recursos en la ruta que espera. Con un motor nativo que espera OBB y DATA externos, empaquetar los bytes dentro del APK sin modificar el loader no lo hará funcional.

### Recomendación

La solución más realista no es un único APK monolítico de 6 GB. Es un **APK instalador/launcher** que incluya o descargue los recursos, valide hashes y los coloque en un almacenamiento controlado por la propia aplicación, junto con un loader modificado para leer esa ruta. Si se exige un único archivo APK sin descarga adicional, habría que integrar el OBB y la DATA como assets y redirigir todas las rutas de lectura; todavía no se debe modificar nada hasta verificar la DATA.

## Archivos de análisis

- `originals/fc27-mahgames-upload-2026-09-12/obb.zip`
- `research/fc27-obb-upload/obb.sha256`
- `research/fc27-obb-upload/obb-zip-list.txt`
- `research/fc27-obb-upload/container-summary.tsv`
- `research/fc27-obb-upload/big4_manifest.json`
- `research/fc27-obb-upload/big4_members.csv`
- `research/inventory_big4_fc27.py`

Ningún archivo original fue modificado. La DATA sigue pendiente.
