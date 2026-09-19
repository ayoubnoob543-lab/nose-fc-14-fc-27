# Informe forense de FIFA 14 Android 1.3.0.3

## Conclusión ejecutiva

El enlace proporcionado de Softonic entrega únicamente un **APK de FIFA 14 versión 1.3.0.3**. No entrega un OBB junto al APK ni aparece un OBB en la descarga del navegador. El APK es un lanzador y contenedor de recursos pequeños; el motor nativo contiene referencias explícitas a los recursos de juego grandes que normalmente deben residir en el OBB o en el directorio de datos del título.

Por tanto, el análisis permite identificar con precisión las **rutas de interfaz que el motor espera**, pero no permite enumerar los miembros internos de cada `.big` porque el OBB no está disponible todavía. No se ha modificado ningún APK, OBB ni archivo original. Los archivos extraídos durante el análisis son copias de trabajo bajo `research/extracted/`.

## Versión y archivos obtenidos

| Campo | Resultado |
|---|---|
| Fuente | [Softonic, página de descarga de FIFA 14 1.3.0.3][1] |
| Versión seleccionada | FIFA 14 Android **1.3.0.3** |
| Tipo descargado | APK |
| APK local | `originals/fifa-14-1.3.0.3-softonic.apk` |
| Tamaño del APK | 18.267.614 bytes |
| SHA-256 del APK | `df5a87532431731f3ef14ae164a4a7b73d29851d4cf5353a62c7057e92188cb8` |
| OBB | **No descargado; el enlace de Softonic no lo proporcionó** |
| Nombre del paquete | `com.ea.game.fifa14_row` |
| Arquitectura declarada | `armeabi` |
| Librería principal | `lib/armeabi/libFIFA14.so` |

Softonic presenta esta descarga como un APK. La ficha de Uptodown para FIFA 14 1.3.6, consultada anteriormente, es una distribución distinta en formato XAPK y declara 1,31 GB; no debe mezclarse con el APK 1.3.0.3 de Softonic. La ficha de Uptodown identifica el mismo paquete `com.ea.game.fifa14_row`.[2]

## Archivos `.big` encontrados

### Archivos físicos encontrados en el APK

**Ninguno.** La lista ZIP del APK no contiene archivos con extensión `.big`, `.obb`, `.apt` ni `.const` como miembros independientes.

### Archivos `.big` referenciados por el motor

La librería `libFIFA14.so` contiene las siguientes referencias de archivos `.big`:

| Referencia | Interpretación probable | Estado |
|---|---|---|
| `data/gui/cro_anim.big` | Animaciones y recursos de la capa gráfica de interfaz | Referenciado por el motor; miembro no disponible en el APK |
| `data/preload/bootpreloaded.big` | Recursos precargados durante arranque | Referenciado por el motor; miembro no disponible en el APK |
| `data/preload/campreloaded.big` | Recursos precargados de campaña/carrera | Referenciado por el motor; miembro no disponible en el APK |
| `data/cmn/ini.big` | Datos comunes de configuración | Referenciado por el motor; miembro no disponible en el APK |
| `data/cmn/ini_psp.big` | Variante de datos comunes | Referenciado por el motor; miembro no disponible en el APK |
| `data/cmn/be/motionintermediate.big` | Datos de gameplay/backend relacionados con movimiento | Referenciado por el motor; miembro no disponible en el APK |
| `data/cmn/be/setplays.big` | Jugadas preparadas y datos de gameplay | Referenciado por el motor; miembro no disponible en el APK |
| `setplays.big` | Alias o ruta alternativa de jugadas | Referenciado por el motor; miembro no disponible en el APK |
| `Speech/dat_ENG_US.big` | Audio/datos de idioma inglés | Referenciado por el motor; miembro no disponible en el APK |
| `Speech/hdr_ENG_US.big` | Cabecera de paquete de audio | Referenciado por el motor; miembro no disponible en el APK |
| `Speech/sth_ENG_US.big` | Datos auxiliares de audio | Referenciado por el motor; miembro no disponible en el APK |
| `aemsbank.big` | Banco de audio | Referenciado por el motor; miembro no disponible en el APK |
| `aemsbank11K.big` | Banco de audio de baja frecuencia | Referenciado por el motor; miembro no disponible en el APK |
| `aemsbank22K.big` | Banco de audio de mayor frecuencia | Referenciado por el motor; miembro no disponible en el APK |
| `chants.big` | Cánticos | Referenciado por el motor; miembro no disponible en el APK |
| `chants11K.big` | Cánticos de baja frecuencia | Referenciado por el motor; miembro no disponible en el APK |
| `chants22K.big` | Cánticos de mayor frecuencia | Referenciado por el motor; miembro no disponible en el APK |

Estas son **referencias extraídas de cadenas de la librería**, no una afirmación de que todos esos archivos estén dentro de un único OBB. El OBB será necesario para confirmar el contenedor concreto y sus miembros.

## Archivos concretos relacionados con menú e interfaz

La siguiente tabla separa las rutas que el motor nombra directamente de las inferencias que requieren confirmar el OBB. La nomenclatura `.apt` y `.const` es consistente con el sistema APT de interfaz de EA: `.apt` contiene datos convertidos de películas Flash y `.const` contiene valores de layout, colores y dimensiones.[3]

| Función | Archivos o rutas identificadas | Nivel de certeza |
|---|---|---|
| Menú principal | `screens/menu/MainMenu`; `data/gui/cro_base`; `data/gui/cro_base_small`; `data/gui/cro_base_micro`; `data/gui/cro_feonly`; `data/gui/cro_anim.big` | Alto para los controladores/rutas; el contenido visual exacto requiere el OBB |
| Menú de pausa | `screens/pauseMenu/PauseMenu` | Alto para el controlador; los gráficos requieren el OBB |
| Botones y elementos seleccionables | `data/gui/icons.png`; `data/gui/icons.coords`; recursos APT/`.const` asociados a los paquetes GUI | Alto para atlas/coordenadas; miembros internos aún no disponibles |
| Iconos | `data/gui/icons.png`; `data/gui/icons.coords`; referencia interna `Apt/icon sprite coords` | Alto |
| HUD de partido | `data/sprites/hud_fui.png`; `data/sprites/hud_fui.sm2`; `layouts.hud`; `mainbe.hud` | Alto para los recursos/rutas nombrados por el motor |
| Marcador | `mainbe.hud`, `layouts.hud` y el código de HUD que usa `SCOREBOARD_OFFSET_X` y `SCOREBOARD_OFFSET_Y` | Medio-alto; no aparece un `scoreboard.big` independiente |
| Fondos de interfaz | `data/sprites/fifa14_splash_%s.sm2`; `data/sprites/loading_sprite.sm2`; `fut/cache/packs/images/packs_backgrounds_%d.png` dentro de `assets/exdata`; fondos adicionales probablemente dentro de los paquetes GUI del OBB | Alto para splash/loading y FUT; incompleto para el menú principal hasta disponer del OBB |
| Fuentes y texto | `data/gui/fonts/`; rutas de locale `data/gui/locale/...`; `.apt` y `.const` | Alto para las rutas; archivos físicos pendientes del OBB |

El APK sí contiene recursos auxiliares pequeños como `Bar_Gradient.png`, `VidButton_Close_108x72.png`, `VidButton_More_108x72.png` y `close-button.png`. No son el menú principal del juego: son recursos auxiliares de la capa Android, vídeo o publicidad.

## Qué habría que modificar para una interfaz estilo EA FC 27

La primera capa de trabajo sería sustituir o reimportar los recursos gráficos del atlas de interfaz, en especial `data/gui/icons.png` y sus coordenadas `data/gui/icons.coords`. Después habría que actualizar los paquetes APT y sus constantes de layout. Los candidatos principales son los paquetes asociados a `data/gui/cro_base`, `cro_base_small`, `cro_base_micro`, `cro_feonly`, `cro_playerheads` y `cro_anim.big`.

Para el menú principal y el menú de pausa habría que mantener la lógica de `MainMenu` y `PauseMenu` o adaptar sus bindings a los nuevos nombres de botones. Para el HUD de partido habría que trabajar con `data/sprites/hud_fui.png`, `hud_fui.sm2`, `mainbe.hud` y `layouts.hud`. El marcador no parece estar aislado en un `.big` con nombre propio; el motor expone offsets de marcador y carga el HUD mediante su controlador. Eso implica que una sustitución visual puede ser suficiente si se conserva el layout esperado, mientras que un cambio de comportamiento exigiría modificar código nativo o scripts.

La recomendación es **no tocar todavía `libFIFA14.so` ni `classes.dex`**. Primero hay que obtener el OBB correcto, inventariar cada `.big`, extraer sus tablas de miembros y localizar los `.apt`, `.const`, atlas y `.hud` reales. Solo después se debe diseñar una sustitución compatible con los tamaños, nombres y offsets que espera el motor.

## OBB y dependencia de `Android/data`

Es viable mantener la mayor parte de los recursos pesados en el OBB oficial de expansión, siempre que el paquete conserve su nombre exacto y Android pueda localizar el archivo en:

```text
/storage/emulated/0/Android/obb/com.ea.game.fifa14_row/
```

Esto evita depender de `Android/data` para los recursos principales. Sin embargo, no se puede garantizar que todo pueda vivir dentro del OBB sin probar el motor. El APK contiene una capa de arranque y el motor puede escribir partidas, cachés, logs, FUT y contenido descargado en almacenamiento externo. En el APK analizado aparecen contenedores `assets/data` y `assets/exdata` con bases de datos WebView, preferencias, cachés, archivos de Matchday y fondos de tienda. Estos contenedores no son el OBB del juego y no deben confundirse con él.

La estrategia más segura para una futura modificación sería:

1. Mantener intacto el APK original como referencia.
2. Mantener un OBB base con los mismos nombres y rutas internas.
3. Reemplazar solo miembros de interfaz dentro del OBB modificado, conservando tamaños, nombres y formatos esperados.
4. Dejar fuera del OBB únicamente los datos que el motor genere dinámicamente, como partidas guardadas, cachés, logs o descargas de servicio.
5. Validar primero en un dispositivo/emulador aislado y no distribuir archivos de EA sin las autorizaciones correspondientes.

## Limitación actual y siguiente insumo necesario

El archivo descargado desde Softonic permite analizar el APK y las referencias del motor, pero **no permite afirmar qué miembros internos de cada `.big` contienen exactamente cada pantalla**. Para cerrar esa parte del informe hace falta el OBB correspondiente a `com.ea.game.fifa14_row`, preferiblemente del mismo build 1.3.0.3. Cuando esté disponible, el script `research/inventory_big.py` puede generar un inventario de miembros y clasificaciones sin modificar el OBB original.

## Archivos de análisis

El repositorio contiene el APK original solo localmente y lo excluye de Git mediante `.gitignore`. Los resultados reproducibles son:

- `research/source/fifa-14-1.3.0.3.apk-unzip-list.txt`
- `research/source/fifa-14-1.3.0.3.apk-members.txt`
- `research/source/libFIFA14_resource_references.txt`
- `research/source/libFIFA14_ui_references.txt`
- `research/source/fifa-14-1.3.0.3.sha256`
- `research/inventory_big.py`

### Referencias

[1]: https://www.softonic.com/descargar/fifa-14/android/post-descarga/v/1.3.0.3?dt=internalDownload "Softonic: FIFA 14 APK 1.3.0.3 para Android"
[2]: https://fifa-14.uptodown.com/android/descargar/106882374 "Uptodown: FIFA 14 Android 1.3.6"
[3]: http://aluigi.zenhax.com/viewtopic.php@t=7372.html ".apt y .const en juegos de EA SPORTS"
[4]: https://rewiki.miraheze.org/wiki/EA_BIG_BIGF_Archive "Especificación del formato de archivos EA BIG/BIGF"
