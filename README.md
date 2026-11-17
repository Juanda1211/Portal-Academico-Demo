# Portal de Gestión Académica — Proyecto POO

Este proyecto parte del código Java de `proyecto_clase_poo 3.zip`, incorpora la estructura de servidor web de `ProyectoPOO-Univesidad.zip` y adapta el lenguaje visual (paleta, tarjetas, tablas, formularios y navegación) de `ProyectoPooConectado 5 css(1).zip`.

Es una aplicación web con backend Java (servidor HTTP propio, sin frameworks) que persiste en archivos de texto con la librería TPA. La interfaz es HTML/CSS/JavaScript servida por el mismo programa Java.

## Ejecutar desde NetBeans
1. Descomprime el ZIP.
2. En NetBeans abre **File > Open Project** y selecciona esta carpeta.
3. Verifica que use JDK 21 y que estén reconocidas las librerías de `librerias/`.
4. Ejecuta **Run Project**. La clase principal está configurada como `edu.unimag.AppWeb`.
5. Entra a `http://localhost:8080`.

Si el puerto 8080 está ocupado, agrega `18080` como argumento de ejecución y abre `http://localhost:18080`.

No necesitas Live Server, Node.js ni Express. `edu.unimag.AppWeb` es el único punto de entrada. No ejecutes dos instancias a la vez sobre la misma carpeta, porque comparten los archivos de persistencia.

## Módulos y rutas de la API

| Módulo | Ruta base | Página |
|---|---|---|
| Profesores | `/api/profesores` | `profesor.html` |
| Estudiantes | `/api/estudiantes` | `estudiantes.html` |
| Facultades | `/api/facultades` | `facultad.html` |
| Materias | `/api/materias` | `materia.html` |
| Periodos académicos | `/api/periodos` | `periodoAcademico.html` |
| Formaciones de profesores | `/api/formaciones` | `formacionProfesor.html` |
| Programas | `/api/programas` | `programa.html` |
| Cursos por periodo | `/api/cursos-periodos` | `cursoPeriodo.html` |
| Asignación de profesores a cursos | `/api/cursos-profesores` | `cursoProfesor.html` |
| Planes de estudio | `/api/planes-estudio` | `planEstudio.html` |
| Inscripciones de estudiantes (`CursoEstudiante`) | `/api/inscripciones` | `inscripciones.html` |
| Materias por plan (`PlanEstudioMateria`) | `/api/plan-materias` | `planMateria.html` |
| Monitorías (`Monitoria`) | `/api/monitorias` | `monitorias.html` |

Los tres últimos módulos usan **clave primaria compuesta** (`modelo/pk`):

- `CursoEstudiante`: curso periodo + estudiante.
- `PlanEstudioMateria`: plan de estudio + materia.
- `Monitoria`: curso periodo + estudiante + profesor.

Reglas que conviene conocer:

- La nota de una **inscripción** va de 0 a 500 y aprueba con 300.
- La nota de una **monitoría** va de 0 a 5 (enteros) y aprueba con 3.
- Un dato inválido responde 400; un conflicto de estado (duplicado, transición no permitida) responde 409.
- Los textos no pueden contener el carácter `¦`, porque es el separador de columnas de los archivos de persistencia.

Para el detalle de reglas, estados y endpoints, ver [`GUIA_DEFENSA.md`](GUIA_DEFENSA.md).

## Estructura
- `src/edu/unimag/`: backend Java.
  - `modelo/`: entidades, claves compuestas (`pk/`), enumeraciones de estado y constantes.
  - `dto/`: objetos de entrada y salida, con sus validaciones.
  - `mapeador/`: conversión entre entidades y DTO.
  - `repositorio/` y `persistencia/`: interfaces de acceso a datos y sus implementaciones sobre TPA.
  - `servicio/`: reglas de negocio.
  - `controlador/`: puente entre las rutas y los servicios.
  - `ensamble/`: servidor HTTP (`ServidorWeb`, `RutaBase`, `Json`) y las rutas `/api/...` en `ensamble/modelos/`.
  - `config/modulos/`: la configuración de cada módulo (repositorio, mapeador y servicio). `AppWeb` los ensambla por niveles de dependencia.
- `src/edu/unimag/AppWeb.java`: entrada del servidor para NetBeans.
- `src/edu/unimag/DiagramaClases.java`: genera el diagrama de clases en `src/diagramaBlaster.puml`.
- `web/`: HTML, CSS, JavaScript e imágenes servidas por Java.
- `web/assets/css/portal/`: hojas de estilo originales del segundo comprimido.
- `web/assets/css/legacy-theme.css`: adaptación visual a las páginas existentes.
- `misPersistencias/`: archivos de datos (`*.txt`), uno por entidad. Se crean al primer arranque si no existen.
- `openspec/`: especificaciones y registro de los cambios realizados.

## Diagrama de clases
Para regenerarlo, ejecuta `edu.unimag.DiagramaClases` desde la raíz del proyecto. Escribe el resultado en `src/diagramaBlaster.puml`.

## Despliegue en la nube (Docker)

GitHub solo guarda el código; no ejecuta Java. Para tener la app en línea, conecta este repositorio a un hosting con soporte para Docker (Render, Railway, Fly.io).

1. Sube el proyecto a GitHub (`git remote add origin <url>` y `git push -u origin main`).
2. En el hosting crea un **Web Service** desde el repositorio y elige el entorno **Docker**; usará el `Dockerfile` de la raíz.
3. El servidor lee el puerto de la variable de entorno `PORT` (por defecto 8080). Un argumento de línea de comandos tiene prioridad.

Probar el contenedor en local:

```bash
docker build -t portal-academico .
docker run --rm -p 8080:8080 portal-academico
```

**Persistencia:** los datos viven en `misPersistencias/` dentro del contenedor. En planes gratuitos el disco se reinicia con cada despliegue o reinicio y vuelven los datos iniciales del repositorio. Para conservar los cambios hay que montar un volumen persistente en `/app/misPersistencias`.
