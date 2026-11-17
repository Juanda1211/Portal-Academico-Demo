# Guía para defender el proyecto

Explicación del proyecto basada en el código final. Cubre la arquitectura por capas del backend, cómo el paquete `ensamble` lo conecta con el frontend (HTML, CSS y JS), y las reglas de negocio de los tres módulos nuevos.

---

## 1. Visión general

Es un portal académico en Java **sin frameworks**: el servidor HTTP es el `HttpServer` que trae el JDK. Persiste en archivos de texto con la librería TPA (`tpa-library-blaster`). El frontend son páginas HTML con CSS y JavaScript, servidas por el mismo programa Java.

Se arranca con `edu.unimag.AppWeb` (puerto 8080, o el que se pase como argumento).

```
   NAVEGADOR                     SERVIDOR JAVA  (edu.unimag.AppWeb)                          DISCO
┌───────────────┐          ┌─────────────────────────────────────────────────────┐
│ HTML + CSS    │  GET /   │ ServidorWeb                                         │
│ + JavaScript  │ ───────► │   ├─ "/"          → entrega archivos de web/        │
│               │          │   └─ "/api/xxx"   → una *Ruta por módulo            │
│  fetch(...)   │  JSON    │         │                                           │
│               │ ◄─────── │   Ruta → Controlador → Servicio → Repositorio ──────┼──► misPersistencias/*.txt
└───────────────┘          │                          └→ Mapeador → DTO          │
                           └─────────────────────────────────────────────────────┘
```

Hay dos tipos de petición, y el mismo servidor atiende ambas:
- **Archivos** (`/`, `/monitorias.html`, `/assets/js/comun.js`...): el servidor los lee de la carpeta `web/` y los entrega.
- **Datos** (`/api/...`): una ruta Java los procesa y responde JSON.

---

## 2. Mapa de paquetes: quién hace qué

| Paquete (`edu.unimag.…`) | Responsabilidad | Ejemplo |
|---|---|---|
| `ensamble` | Puente con el exterior: HTTP, JSON, rutas | `ServidorWeb`, `RutaBase`, `Json` |
| `ensamble.modelos` | Una ruta `/api/...` por módulo | `MonitoriaRuta` |
| `controlador` | Punto de entrada al negocio; delega al servicio | `MonitoriaControlador` |
| `servicio` | Reglas de negocio | `MonitoriaServicio` |
| `mapeador` | Convierte entidad → DTO | `MonitoriaMapeador` |
| `dto` | Datos de entrada y salida, con validación | `MonitoriaActualizarDto` |
| `modelo` | Entidades y sus reglas internas | `Monitoria` |
| `modelo.pk` | Claves primarias compuestas | `MonitoriaPk` |
| `modelo.enumeracion` | Estados y máquinas de estado | `EstadoMonitoria` |
| `repositorio` | Interfaces de acceso a datos | `MonitoriaRepositorio` |
| `repositorio.cargador` | Cargan entidades relacionadas con caché | `EstudianteCargador` |
| `persistencia` | Implementación sobre TPA | `MonitoriaRepositorioImpl` |
| `config`, `config.modulos` | Construyen repositorio + mapeador + servicio de cada módulo | `ConfiguracionModuloMonitoria` |
| `AppWeb` | Ensambla todo y arranca el servidor | |

### La regla de dependencias

Las capas solo conocen a la que tienen debajo. Lo comprobé revisando los `import` del código:

| Capa | Lo que **no** importa |
|---|---|
| `modelo` | dto, servicio, controlador, repositorio, persistencia, ensamble, mapeador |
| `dto` | servicio, controlador, repositorio, persistencia, ensamble, mapeador |
| `repositorio` (interfaces) | servicio, controlador, ensamble, persistencia |
| `persistencia` | servicio, controlador, ensamble, dto, mapeador |
| `servicio` | persistencia, controlador, ensamble |
| `controlador` | repositorio, persistencia, ensamble, modelo |
| `ensamble.modelos` (rutas) | solo importan `controlador`, `dto` y `ensamble` |

Dos consecuencias para la defensa:
- **El modelo no sabe que existe una web.** Se podría reutilizar con otra interfaz, como pasó con la consola que se eliminó.
- **Las rutas no conocen servicios ni entidades.** Solo hablan con controladores y DTO.

Una excepción que conviene conocer: los mapeadores importan los **cargadores** (`repositorio.cargador`), porque necesitan completar los datos de las entidades referenciadas (ver sección 8).

---

## 3. Recorrido completo de una petición

### Ejemplo A: evaluar una monitoría (escritura)

El usuario escribe la nota 4 y un comentario en el modal de `monitorias.html` y pulsa "Registrar evaluación".

| # | Dónde | Qué pasa |
|---|---|---|
| 1 | `monitorias.js` | El formulario valida en el cliente (entero de 0 a 5) y llama `peticionApi('/api/monitorias/{curso}/{est}/{prof}', 'PUT', {calificacion, comentario})` |
| 2 | `comun.js` | `peticionApi` hace `fetch` con `Content-Type: application/json` y el cuerpo convertido a JSON |
| 3 | `ServidorWeb` | Tiene un `createContext("/api/monitorias", ...)` registrado por `AppWeb`: entrega la petición a `MonitoriaRuta` |
| 4 | `RutaBase.handle` | Parte la URL en `["cursoUUID", "estUUID", "1"]` y llama al método abstracto `manejar(ex, "PUT", partes)` |
| 5 | `MonitoriaRuta.manejar` | Convierte los 3 ids del URL (`idUuid`, `idLong`), los suma al cuerpo y llama `Json.aRecord(MonitoriaActualizarDto.class, datos)` |
| 6 | `MonitoriaActualizarDto` | Su constructor valida: ids obligatorios, nota entre 0 y 5, comentario obligatorio. Si algo falla, lanza `IllegalArgumentException` |
| 7 | `MonitoriaControlador.evaluar` | Delega en `MonitoriaServicio.evaluar` |
| 8 | `MonitoriaServicio` | Busca la monitoría por su clave compuesta (si no existe, error) y llama `monitoria.evaluar(nota, comentario)` |
| 9 | `Monitoria` (modelo) | Calcula el estado con `EstadoMonitoria.desdeCalificacion` (≥ 3 aprueba), valida la transición con `cambiarEstadoA`, agrega la traza de reevaluación si ya había nota y fija la fecha |
| 10 | `MonitoriaRepositorioImpl` | `actualizar` → `TpaRepository.update` reescribe la fila en `monitorias.txt` |
| 11 | `MonitoriaMapeador` | Completa estudiante, curso, materia, periodo y profesor con los cargadores y arma el `MonitoriaDto` |
| 12 | `RutaBase.responder` | `Json.aJson(dto)` genera el JSON y se envía con código 200 |
| 13 | `monitorias.js` | Recibe el JSON, cierra el modal y llama `cargarTabla()` para repintar la tabla |

### Ejemplo B: listar inscripciones por materia (lectura)

`GET /api/inscripciones?materia=1` → `CursoEstudianteRuta` lee el parámetro con `query(...)` → `CursoEstudianteControlador.listarPorMateria` → `CursoEstudianteServicio`. El servicio busca los cursos de esa materia, filtra las inscripciones de esos cursos, completa los datos con los cargadores y los convierte a DTO → lista JSON → `inscripciones.js` pinta una fila por inscripción.

---

## 4. Las capas, una por una

### 4.1 Modelo (`modelo`, `modelo.pk`, `modelo.enumeracion`)
- Las entidades **tienen comportamiento**: no son bolsas de datos. `CursoEstudiante` expone `admitir()`, `evaluar()`, `retirar()`, `rechazar()`; `PlanEstudioMateria` tiene `reubicarSemestre()` y `actualizarOrden()`; `Monitoria` tiene `evaluar()`.
- Los enums son **máquinas de estado**: `cambiarEstadoA(nuevo)` solo acepta transiciones permitidas y lanza `IllegalStateException` si no.
- Las claves compuestas son clases aparte (`CursoEstudiantePk`, `PlanEstudioMateriaPk`, `MonitoriaPk`) con campos finales y `equals`/`hashCode`. En la entidad se declaran con `@TpaId`.

### 4.2 DTO (`dto`)
- Son `record` de Java: inmutables.
- **La validación vive en el constructor compacto** del record, usando `ReglasValidacion`. Así, un dato inválido no llega nunca al servicio.
- Cada módulo tiene tres: `Dto` (salida), `CrearDto` y `ActualizarDto` (entrada). Los módulos nuevos siguen el mismo patrón. Por ejemplo, `CursoEstudianteActualizarDto` es el que se usa para evaluar una inscripción y `MonitoriaActualizarDto` para evaluar una monitoría.

### 4.3 Repositorio y persistencia
- `repositorio/*Repositorio` son **interfaces**: dicen qué se puede hacer (`guardar`, `buscarPorId`, `listarPorPlan`...). El servicio depende de ellas, no de la implementación.
- `persistencia/*RepositorioImpl` las implementan sobre `TpaRepository`.
- `RepositorioBaseAbstracto<T, ID>` evita repetir código: implementa `guardar`, `actualizar`, `buscarPorId`, `listarTodos`, `contar`, `eliminar` y `cerrar` delegando en TPA.
- `eliminar` es `final` y lanza `UnsupportedOperationException` salvo que la subclase sobrescriba `permiteBorradoFisico()` → `true`. Es un **método plantilla**: por defecto no se borra nada. Solo `PlanEstudioMateria` y `Monitoria` lo habilitan.
- `RutaPersistencia` (enum) asocia cada entidad con su archivo en `misPersistencias/`.

### 4.4 Servicio (`servicio`)
- Aquí viven las **reglas de negocio**: que exista el estudiante, que no haya duplicados, que el estudiante esté inscrito antes de ser monitor, que el orden no se repita.
- Recibe las interfaces de repositorio, los mapeadores y devuelve DTO. Nunca devuelve entidades hacia arriba.

### 4.5 Mapeador y cargadores
- El **mapeador** convierte entidad → DTO.
- Los **cargadores** (`CargadorDatos<T, ID>`) leen entidades relacionadas con una caché: `cargarPorIds(lista)` trae varias de una vez y `obtener(id)` consulta la caché. Así se evita releer el archivo una vez por fila.

### 4.6 Controlador (`controlador`)
- Es deliberadamente delgado: recibe DTO, llama al servicio, devuelve DTO. No conoce repositorios, persistencia, entidades ni HTTP.
- Su valor es separar "cómo llega la petición" de "qué hace el negocio".

---

## 5. El paquete `ensamble`: el puente hacia el frontend

Es el único paquete que sabe de HTTP y de JSON. Tiene tres clases y una subcarpeta con las rutas.

### 5.1 `ServidorWeb`: un servidor con dos trabajos
Usa el `HttpServer` del JDK.
1. **Archivos estáticos.** Un contexto `"/"` entrega lo que haya en `web/`:
   - `/` se convierte en `/index.html`.
   - Calcula el tipo MIME por extensión (`html`, `css`, `js`, `png`, `svg`...).
   - Envía `Cache-Control: no-cache`, así el navegador siempre pide la versión actual.
   - **Seguridad:** normaliza la ruta y rechaza cualquier archivo fuera de `web/` (protección contra `../`).
2. **API.** `registrar("/api/monitorias", ruta)` crea un contexto adicional por módulo. El servidor elige el contexto más específico, por eso `/api/...` va a las rutas y todo lo demás a los archivos.

Otro detalle de diseño: el servidor usa **un solo hilo** (`newSingleThreadExecutor`). Es deliberado: los archivos `.txt` no soportan escrituras simultáneas, y un solo hilo serializa todas las peticiones.

### 5.2 `RutaBase`: lo repetitivo de todas las rutas
Es la clase padre de las 13 rutas. Resuelve lo común para que cada ruta solo escriba su lógica.
- `handle(...)` parte la URL: `/api/materias/3/suspender` → `["3", "suspender"]`.
- Llama al método abstracto `manejar(ex, metodo, partes)`, que cada ruta implementa.
- Ofrece utilidades: leer el cuerpo, `query(...)` para parámetros, `idUuid`/`idLong`/`idEntero` para convertir ids, `responder(...)`, `noPermitido`, `noEncontrado`.
- **Traduce excepciones a códigos HTTP** (la tabla clave de la defensa):

| Excepción | HTTP | Sentido |
|---|---|---|
| `IllegalArgumentException` | **400** | Dato inválido o entidad inexistente |
| `IllegalStateException` | **409** | Petición válida que choca con el estado actual |
| Ruta o acción desconocida | 404 | |
| Método no permitido | 405 | |
| Cualquier otra (`Throwable`) | 500 | Error interno; imprime la traza |

- Tiene una **red de seguridad**: si una ruta termina sin responder, devuelve 500 en lugar de dejar al navegador esperando.
- Cierra la conexión en un `finally`, después de responder.

### 5.3 `Json`: convertidor propio
No usa Jackson ni Gson; es una clase escrita a mano con tres funciones:
- **`aJson(objeto)`**: serializa `record`, `Map`, listas, enums (por su nombre, ej. `"PENDIENTE"`), números y fechas (ISO-8601). Los DTO de salida se convierten directo a JSON.
- **`leerObjeto(texto)`**: parser que convierte el cuerpo JSON de la petición en un `Map`.
- **`aRecord(Clase, mapa)`**: crea el DTO llamando a su constructor por reflexión. **Por eso las validaciones del DTO corren igual que en cualquier otro uso**: no hay un camino que se salte la validación.
- Además, como único punto de entrada de textos, aplica `textoSeguro`: rechaza `¦` (400) y convierte saltos de línea en espacio, porque romperían el archivo de persistencia.

### 5.4 Anatomía de una ruta (`ensamble/modelos/*Ruta`)
Cada ruta hace tres cosas, en este orden: decide por el **método HTTP y la forma de la URL** qué operación es, arma el DTO con `Json.aRecord`, y llama **solo al controlador**. Ejemplo (`MonitoriaRuta`):

| URL y método | Operación |
|---|---|
| `POST /api/monitorias` | crear |
| `GET /api/monitorias` (con o sin `?curso=`, `?estudiante=`, `?profesor=`) | listar |
| `PUT /api/monitorias/{c}/{e}/{p}` | evaluar |
| `DELETE /api/monitorias/{c}/{e}/{p}` | eliminar |
| otra forma de URL | 404 / 405 |

### 5.5 `AppWeb`: dónde se arma todo
`AppWeb` es la **raíz de composición**: el único lugar donde se hacen los `new` de los módulos. Es inyección de dependencias manual, sin framework.

1. **Módulos por niveles, según sus dependencias.** Un módulo no puede construirse antes que aquellos de los que depende:
   - Nivel 1: Profesor, Estudiante, Facultad, Materia, Periodo.
   - Nivel 2: Formación, Programa.
   - Nivel 3: CursoPeriodo.
   - Nivel 4: PlanEstudio.
   - Nivel 5: CursoProfesor, CursoEstudiante.
   - Nivel 6: PlanEstudioMateria, Monitoria.
2. Cada `ConfiguracionModulo*` crea su repositorio (con `TpaRepositoryFactory`), su mapeador y su servicio, y los expone con getters. Los módulos dependientes reciben los repositorios que necesitan por el constructor.
3. Se crean los 13 controladores con los servicios.
4. Se crea `ServidorWeb(puerto, carpeta web)` y se registran las 13 rutas `/api/...`.
5. Un *shutdown hook* detiene el servidor y cierra los repositorios al terminar.

---

## 6. El frontend: HTML, CSS y JavaScript

### 6.1 Qué hay en `web/`
- 14 páginas HTML (`index.html` y una por módulo).
- `assets/css/`, `assets/js/` (un `.js` por página más `comun.js`) y `assets/img/`.

### 6.2 Anatomía de una página (ejemplo: `monitorias.html`)
1. `<head>`: enlaza las hojas de estilo y carga los scripts con `defer`: primero `comun.js` y luego el de la página (`monitorias.js`).
2. `<header>` con el menú de navegación (dos filas de 7 enlaces) y el botón de menú móvil.
3. `<main>`: una tarjeta con la barra de filtros, el botón de crear y una `<table>` cuyo `<tbody>` está vacío y tiene un `id`. El JS llena ese cuerpo.
4. Uno o dos modales (`modal-overlay`) con sus formularios.
5. `<footer>`.

El HTML es solo esqueleto: **los datos nunca están en el HTML**; llegan por la API.

### 6.3 Organización del CSS
Las hojas se cargan en capas, de lo general a lo específico:

| Archivo | Para qué |
|---|---|
| `base.css` | Variables generales, reglas básicas (`body`, `a`, `.contenedor`) |
| `entidades.css` | Variables de color de las entidades |
| `estructura.css` | Cabecera, navegación, desplegables y pie |
| `componentes.css` | Botones, alertas, tarjetas y tablas del portal |
| `paginas/formulario.css`, `paginas/lista.css` | Campos y avisos de formulario; listados |
| `style.css` | Botones y badges de las tablas CRUD (`.btn`, `.btn-tabla`, `.badge`) |
| `legacy-theme.css` | Paleta (azul marino y dorado) y clases del diseño anterior (`.encabezado-tabla`, `.campo-grid`) |
| `adaptacion-proyecto-java.css` | Ajustes mínimos: tarjetas de servicios del inicio y regla del menú móvil |

La barra de navegación en dos filas está en `estructura.css`: usa una rejilla de dos filas de igual altura, y en pantallas menores de 768 px se aplana en una columna.

### 6.4 El patrón de cada página JS
Todas siguen el mismo ciclo:
1. **`cargarCatalogos()`**: llena los `<select>` con otros endpoints (por ejemplo, la lista de estudiantes para elegir un monitor).
2. **`cargarTabla()`**: `peticionApi(GET)` → recibe un arreglo de DTO → construye una fila por elemento con plantillas de texto.
3. **Eventos**: al enviar un formulario llama `peticionApi(POST/PUT)`, y si sale bien cierra el modal y vuelve a llamar `cargarTabla()`.
4. Los botones de cada fila **dependen del estado**: una inscripción Pendiente muestra Admitir y Rechazar; una Admitida muestra Evaluar y Retirar.
5. Los textos que vienen del servidor se escapan antes de insertarse en el HTML en las páginas nuevas (`escapar`), para evitar inyección de código.

### 6.5 `comun.js` y `peticionApi`: el pegamento
```
peticionApi(ruta, metodo = 'GET', cuerpo = null)
```
- Hace `fetch` a una ruta **relativa** (`/api/...`): como la página y la API salen del mismo servidor y puerto, no hay problemas de CORS.
- Envía y espera JSON.
- Si la respuesta no es 2xx, lanza un `Error` con el mensaje que mandó Java (`{"error": "..."}`), para que la página lo atrape y se lo muestre al usuario.
- Además maneja el estado de carga del botón y otras utilidades comunes (menú móvil, marcar la página actual en el menú, buscador por texto).

---

## 7. El contrato entre frontend y backend

Ejemplo real, crear una inscripción:

**Petición:** `POST /api/inscripciones`
```json
{ "idCursoPeriodo": "uuid-del-curso", "idEstudiante": "uuid-del-estudiante" }
```
**Respuesta 201:** el DTO ya con las entidades completas anidadas
```json
{ "estudiante": { "id": "…", "codigo": "E001", "nombre": "Ana", … },
  "cursoPeriodo": { "id": "…", "nombre": "Calc-01",
                    "materia": { "nombre": "Calculo", … },
                    "periodo": { "nombre": "2026-1", … } },
  "estado": "PENDIENTE", "calificacion": null,
  "fechaInscripcion": "2026-11-14T…", "fechaEvaluacion": null }
```
**Error 409:** `{ "error": "El estudiante ya esta inscripto en este curso periodo" }`

### Camino completo de un error

| Dónde nace | Qué se lanza | Qué hace `RutaBase` | Qué ve el usuario |
|---|---|---|---|
| Un DTO rechaza un dato | `IllegalArgumentException` | 400 + `{"error":…}` | Mensaje en el modal (`#mensajeError`) |
| El servicio no encuentra la entidad | `IllegalArgumentException` | 400 | Igual |
| El modelo rechaza una transición | `IllegalStateException` | 409 | Mensaje en el modal o `alert` si fue un botón de fila |
| Falla inesperada | cualquier otra | 500 + traza en consola | Mensaje de error genérico |

---

## 8. Persistencia y la "regla de un salto"

Cada entidad vive en un archivo de `misPersistencias/`, una fila por línea:
- Las columnas van separadas por `¦`.
- Cada fila termina en `~`.
- El valor nulo se escribe `#_NULL_#`.

Los archivos se crean al primer arranque si no existen.

**Regla de un salto.** Al leer una fila, TPA reconstruye las entidades referenciadas con **solo su id**; sus demás campos vienen en `null`. Por eso existen los cargadores: el servicio los usa para completar esos datos y el mapeador arma el DTO completo. Así se logran las cadenas de la pauta:
- `CursoPeriodo → Materia, PeriodoAcademico`
- `PlanEstudio → Programa → Facultad`

Consecuencia: el repositorio de inscripciones no puede filtrar por materia directamente. El servicio cruza con `CursoPeriodoRepositorio` para saber qué cursos son de esa materia.

---

## 9. Los tres modelos nuevos

| Modelo | PK compuesta | Atributos propios |
|---|---|---|
| `CursoEstudiante` | `CursoPeriodo` + `Estudiante` | estado, calificación, fecha de inscripción, fecha de evaluación |
| `PlanEstudioMateria` | `PlanEstudio` + `Materia` | semestre, orden |
| `Monitoria` | `CursoPeriodo` + `Estudiante` + `Profesor` | estado, calificación, comentario, fecha de creación, fecha de evaluación |

### Máquina de estados de la inscripción (`EstadoInscripcion`)
```
PENDIENTE ──admitir──► ADMITIDA ──evaluar ≥ 300──► APROBADA ──retirar──► RETIRADA
    │                     │
    │                     ├──evaluar < 300──► REPROBADA
    │                     └──retirar────────► RETIRADA
    └──rechazar──► RECHAZADA
```
REPROBADA, RETIRADA y RECHAZADA son finales. La nota va de **0 a 500** y aprueba con 300.

### Máquina de estados de la monitoría (`EstadoMonitoria`)
```
PENDIENTE ──evaluar──► APROBADO ⇄ REPROBADO   (también se puede reevaluar con el mismo resultado)
```
La nota va de **0 a 5, solo enteros**, y aprueba con 3. Cuando se reevalúa, el comentario anterior se conserva y se agrega ` | REEVALUACIÓN [fecha]: comentario (Nota anterior: N)`, una traza de auditoría.

---

## 10. Las 19 funcionalidades

### Inscripciones de estudiantes (10)

| # | Función | Endpoint |
|---|---|---|
| 1 | Inscribir estudiante a curso | `POST /api/inscripciones` |
| 2 | Listar cursos por estudiante | `GET /api/inscripciones?estudiante=` |
| 3 | Listar estudiantes por curso periodo | `GET /api/inscripciones?curso=` |
| 4 | Listar estudiantes por materia | `GET /api/inscripciones?materia=` |
| 5 | Listar por materia y periodo | `GET /api/inscripciones?materia=&periodo=` |
| 6 | Listar por periodo | `GET /api/inscripciones?periodo=` |
| 7 | Admitir inscripción | `POST /api/inscripciones/{curso}/{est}/admitir` |
| 8 | Evaluar inscripción | `PUT /api/inscripciones/{curso}/{est}/evaluar` con `{calificacion}` |
| 9 | Retirar estudiante | `POST /api/inscripciones/{curso}/{est}/retirar` |
| 10 | Rechazar inscripción | `POST /api/inscripciones/{curso}/{est}/rechazar` |

El `GET` sin ningún filtro devuelve todas las inscripciones.

### Asignaciones de materias a planes (5)

| # | Función | Endpoint |
|---|---|---|
| 1 | Asignar materia a plan | `POST /api/plan-materias` |
| 2 | Listar materias de un plan | `GET /api/plan-materias?plan=` |
| 3 | Listar planes que incluyen una materia | `GET /api/plan-materias?materia=` |
| 4 | Actualizar semestre u orden | `PUT /api/plan-materias/{plan}/{materia}` con campos opcionales |
| 5 | Quitar materia del plan | `DELETE /api/plan-materias/{plan}/{materia}` |

El `GET` sin plan ni materia responde 400.

### Monitorías (4)

| # | Función | Endpoint |
|---|---|---|
| 1 | Crear monitoría | `POST /api/monitorias` |
| 2 | Evaluar monitoría | `PUT /api/monitorias/{curso}/{est}/{prof}` con `{calificacion, comentario}` |
| 3 | Eliminar monitoría | `DELETE /api/monitorias/{curso}/{est}/{prof}` |
| 4 | Listar monitorías | `GET /api/monitorias` o con `?curso=`, `?estudiante=`, `?profesor=` |

---

## 11. Reglas de negocio

- **Inscripción duplicada:** 409.
- **Monitoría:**
  - El estudiante debe estar inscrito en ese curso, y la inscripción no puede estar retirada ni rechazada (409).
  - No se puede duplicar la misma combinación curso–estudiante–profesor (409).
  - El profesor debe existir (400).
- **Plan–materia:**
  - Una materia no puede repetirse dentro de un plan (409).
  - El `orden` es único por semestre dentro del plan (409).
  - Semestre y orden deben ser ≥ 1 (400).
  - Actualizar exige al menos un campo y que haya un cambio real (400).
- **Borrado:**
  - `PlanEstudioMateria` y `Monitoria` se borran físicamente: son filas de asociación.
  - `CursoEstudiante` **nunca** se borra. "Retirar" cambia el estado, para conservar el historial académico.

---

## 12. Cambios hechos al final

- **Nota de monitoría de 0 a 5.** Las constantes están en `EstadoMonitoria`, no en `PoliticasAcademicas`, para no tocar un archivo compartido. La inscripción conserva 0–500.
- **Reevaluar con el mismo estado** (por ejemplo de 3 a 4) respondía 409. Se corrigió en `EstadoMonitoria.cambiarEstadoA`.
- **Caracteres reservados.** Un texto con `¦` o saltos de línea corrompía el archivo y dejaba todo el listado de esa entidad en 500. Ahora `¦` responde 400 y los saltos se convierten en espacio (`Json.textoSeguro`).
- **Listado de inscripciones sin filtro** ahora muestra todas.
- **Navbar** en dos filas de 7 enlaces, igual en las 14 páginas.
- **Se eliminaron las vistas de consola y `App.java`.** En las configuraciones, `construirVista()` devuelve `null`; solo funciona la web.

---

## 13. Preguntas probables

### Sobre la arquitectura y la conexión con el frontend

**¿Cómo se conecta el HTML con el código Java?**
Por HTTP. El JavaScript de la página hace `fetch('/api/...')`; `ServidorWeb` entrega esa petición a una ruta del paquete `ensamble`, que llama al controlador. La respuesta vuelve como JSON y el JS la pinta en la tabla. El HTML nunca contiene datos.

**¿Qué hace exactamente el paquete `ensamble`?**
Es la frontera con el exterior. Recibe HTTP, lo traduce a DTO (con `Json`), llama al controlador, y traduce el resultado (o la excepción) otra vez a HTTP. El resto del backend no sabe que existe HTTP.

**¿Por qué existe `RutaBase`?**
Para no repetir en 13 rutas lo mismo: partir la URL, leer el cuerpo, responder JSON y convertir excepciones en códigos HTTP. Cada ruta solo implementa `manejar`.

**¿Por qué escribiste tu propio `Json`?**
El proyecto usa solo el JDK y la librería TPA, sin dependencias para JSON. La clase es pequeña (serializa `record`, enums, listas, fechas) y permite que `aRecord` cree el DTO por su constructor, así las validaciones nunca se saltan.

**¿Por qué el servidor usa un solo hilo?**
Porque la persistencia son archivos `.txt` que no soportan escrituras simultáneas. Un solo hilo serializa las peticiones y evita corrupción. A cambio, no escala; para este proyecto es lo correcto.

**¿Por qué `AppWeb` crea todos los objetos con `new`?**
Es inyección de dependencias manual: un único lugar, la raíz de composición, conoce las clases concretas y las conecta. El resto recibe sus dependencias por el constructor y depende de interfaces de repositorio.

**¿Por qué las rutas no llaman al servicio directamente?**
Para que quien maneja HTTP no dependa del negocio. El controlador es la única puerta de entrada al negocio, así que cambiar la interfaz (web, consola) no toca el servicio.

**¿Por qué fetch a rutas relativas y no a una URL completa?**
La página y la API salen del mismo servidor y puerto, así que no hay CORS ni hace falta configurar nada.

### Sobre el diseño

**¿Por qué la PK compuesta es una clase aparte?**
Porque la identidad es la combinación de dos o tres entidades. La clase PK permite compararlas y guardarlas como un solo id.

**¿Por qué DTOs y no devolver la entidad?**
Para validar la entrada en el constructor del `record` y para no exponer el modelo interno.

**¿Por qué existe el mapeador?**
Para aislar la conversión entidad → DTO. El servicio no sabe de JSON ni del formato de salida.

**¿Dónde están las reglas de negocio?**
En el modelo (transiciones de estado) y en el servicio (existencias, duplicados). Nunca en la ruta ni en el JS. El JS valida lo básico para dar respuesta rápida, pero el backend siempre vuelve a validar.

**¿Por qué "retirar" en vez de borrar una inscripción?**
Para conservar el historial académico del estudiante.

**¿Por qué dos escalas de nota (0–500 y 0–5)?**
Fue el requerimiento: la monitoría usa 0–5 y la inscripción conserva 0–500. Es una diferencia consciente.

**¿Por qué 400 y 409 en vez de un solo código?**
400 indica que la petición está mal o referencia algo que no existe. 409 indica que la petición es válida pero choca con el estado actual.

**¿Qué principios SOLID se cumplen?**
- *Responsabilidad única:* cada capa hace una sola cosa.
- *Abierto/cerrado:* se agregaron tres módulos sin modificar los existentes.
- *Inversión de dependencias:* los servicios dependen de interfaces de repositorio, pero servicios y controladores son clases concretas, sin interfaz. Es una brecha real y conviene reconocerla.

**¿Cómo se probó?**
Con una prueba de API de 99 comprobaciones (altas, errores, valores límite, hidratación), con Chrome DevTools sobre las 14 páginas y con un agente independiente que repitió la verificación.

---

## 14. Limitaciones que conviene conocer

- Servicios y controladores sin interfaz (ver DIP arriba).
- `ConfiguracionDependencias` (en `config`) ya no la usa nadie: servía al `App` de consola. La web arma todo en `AppWeb`.
- Los `construirVista()` de las configuraciones devuelven `null`: son un resto de la consola.
- Quedan otros restos de la consola: `AppScannerCli.java` y `vista/constante/ConfiguracionCli.java`.
- Un solo hilo y sin autenticación: es una aplicación local de clase, no está pensada para producción.
- `ServidorWeb` comenta que solo acepta conexiones locales, pero `new InetSocketAddress(puerto)` enlaza todas las interfaces. El comentario no coincide con el código.
- La carpeta `test/` está vacía: la verificación se hizo con scripts externos y no con pruebas dentro del proyecto.
- El diagrama `src/diagramaBlaster.puml` se regenera ejecutando `edu.unimag.DiagramaClases` desde la raíz del proyecto.
- Errata del docente: "Asginar" en el título de `CursoProfesorVista`, una vista que ya no existe.

---

## 15. Demostración en 2 minutos

1. Abrir `inscripciones.html` y crear una inscripción.
2. Mostrar en la pestaña Red de las herramientas del navegador el `POST /api/inscripciones` y su respuesta JSON: es la conexión frontend–backend a la vista.
3. Admitirla, evaluarla con 350 y comprobar que pasa a Aprobada.
4. Intentar admitirla otra vez para mostrar el 409 y el mensaje en pantalla.
5. Crear una monitoría, evaluarla con 2 (Reprobado) y luego con 4 (Aprobado), y mostrar la traza en el comentario.
6. Asignar una materia a un plan y repetir el mismo orden en el mismo semestre para mostrar el rechazo.
