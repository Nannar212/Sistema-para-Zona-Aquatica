# Cómo crear el modelo 3D de la piscina

El sistema ya tiene todo listo para **mostrar un recorrido 3D** en la página pública
(la gente lo puede girar y hacer zoom desde el navegador o el celular). Solo falta
crear el archivo del modelo. Aquí tienes el paso a paso, de lo más fácil a lo más
avanzado.

El formato que necesitas subir es **`.glb`** (un solo archivo que incluye el modelo
y las texturas).

---

## Opción 1 — Con el celular (la más fácil, recomendada)

Se llama **fotogrametría / escaneo 3D**: tomas muchas fotos o un video moviéndote
alrededor del lugar y una app arma el modelo 3D.

Apps recomendadas (gratis o con prueba):

- **Polycam** (iPhone y Android) — muy fácil, exporta a `.glb`.
- **KIRI Engine** (iPhone y Android) — gratis, exporta a `.glb`.
- **Scaniverse** (iPhone y Android) — gratis, excelente calidad.
- Si tienes un **iPhone/iPad Pro con LiDAR**, el escaneo es aún mejor y más rápido.

Pasos:

1. Instala una de esas apps.
2. Elige el modo **"Photo" / "Photogrammetry"** (o LiDAR si tu equipo lo tiene).
3. Camina **alrededor** de la zona (la piscina, el ingreso, los vestidores, etc.)
   tomando fotos desde muchos ángulos, con **buena luz** y sin gente moviéndose.
   Mientras más fotos y ángulos, mejor queda.
4. Deja que la app procese el modelo.
5. **Exporta** el resultado en formato **`.glb`** (todas estas apps lo permiten).

> Consejo: puedes escanear solo la zona más bonita (la piscina principal) para que
> el archivo no sea muy pesado. Ideal que quede por debajo de **30–40 MB**.

---

## Opción 2 — Modelado a mano (si buscas algo estilizado)

Si prefieres un modelo "de diseño" (no una foto real), alguien con conocimientos
puede armarlo en:

- **Blender** (gratis) — luego *File → Export → glTF 2.0 (.glb)*.
- **SketchUp** — modelar y exportar/convertir a `.glb`.

Esto requiere saber modelar, así que normalmente conviene la Opción 1.

---

## Optimizar el archivo (para que cargue rápido)

Los modelos escaneados suelen ser pesados. Antes de subirlo, conviene reducirlo:

- **https://gltf.report** o **https://sandbox.babylonjs.com** — para revisarlo.
- **https://gltf.pmnd.rs** (gltfjsx) o la herramienta **gltf-transform** — para
  comprimir. Un comando típico (si tienes Node instalado):

  ```bash
  npx @gltf-transform/cli optimize entrada.glb salida.glb --texture-compress webp
  ```

Meta: que el `.glb` final pese **menos de ~30 MB** para que cargue bien en celulares.

---

## Subirlo al sistema

1. Entra como **administrador** → menú **"Página pública"**.
2. Baja hasta **"Recorrido 3D de las instalaciones"** y pulsa **"Subir modelo (.glb)"**.
3. Elige tu archivo `.glb` y espera a que suba.
4. Pulsa **"Guardar"**.
5. Abre la página pública: verás la sección **"Nuestras instalaciones"** debajo de la
   ubicación, con el modelo que se puede **girar y hacer zoom**. En celular incluso
   aparece un botón para verlo en **realidad aumentada (AR)**.

---

## Alternativa sencilla: tour 360°

Si el modelo 3D te parece mucho por ahora, otra forma muy usada de "mostrar por
dentro" es un **tour de fotos 360°** con apps como **Kuula** o **Momento360**. No está
integrado como el 3D, pero podrías pegar el enlace del tour en la descripción o en
las redes. Si más adelante lo quieres integrado, avísame y lo agrego.
