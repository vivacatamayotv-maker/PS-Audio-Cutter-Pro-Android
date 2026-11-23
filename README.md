# PS Audio Cutter Pro Android

Editor de audio independiente, desarrollado en Kotlin con WebView local, para Android 8 o superior.

**Funciones beta**: importar audio, cortar fragmentos, dividir por X partes, dividir por duración, eliminar silencios largos y guardar MP3 (128/192/256 kbps). El procesamiento se realiza localmente, sin subir grabaciones a servidores.

El codificador LameJS se descarga durante la **compilación** desde su repositorio público y se incluye en el APK, para que la aplicación instalada trabaje sin internet. Licencia LGPL-3.0: https://github.com/zhuker/lamejs

**Compilación**: Acciones > Compilar PS Audio Cutter APK > última ejecución > Artefactos > PS-Audio-Cutter-Pro-APK. Dentro del ZIP aparecerá app-debug.apk solamente cuando la compilación termine correctamente.

**Estado**: Proyecto beta, todavía sin APK comprobado en un teléfono real. El navegador Android puede no decodificar todos los formatos. Para entrevistas extensas, el procesamiento en memoria puede requerir mucha RAM. El archivo original nunca se modifica.

La marca tipográfica Pepe Simancas Capa está incluida. El PNG oficial no se incorpora en esta primera subida del repositorio.
