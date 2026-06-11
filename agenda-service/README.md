# Agenda Service (Python/Django)

Versión en Python del módulo `AgendaController.java` de Classify 2.0.
Vive dentro del proyecto (`Classify2.0/agenda-service/`), corre como servicio
independiente y usa la **misma base de datos PostgreSQL** que la app Spring Boot. No modifica el esquema (la tabla `agendas` la sigue
administrando Hibernate).

## Equivalencias

| Java (Spring Boot)                  | Python (Django)        |
|-------------------------------------|------------------------|
| `AgendaController.java`             | `agenda/views.py`      |
| `AgendaService.java`                | `agenda/services.py`   |
| `AgendaRepository.java` (JPA)       | ORM de Django          |
| `Agenda.java` (entidad)             | `agenda/models.py`     |
| Apache POI (Excel)                  | openpyxl               |

## Endpoints (mismas rutas)

- `GET /` — página de inicio: estado del servicio, agendas registradas y
  botón de descarga del Excel. Ábrela en **http://localhost:8081/**.
- `POST /guardar-agenda` — guarda la agenda. Con header
  `X-Requested-With: XMLHttpRequest` responde JSON `{success, message}`
  (igual que espera `modalagenda.js`); sin él, renderiza confirmación HTML.
- `GET /programacion/exportarExcel` — descarga `programacion.xlsx` con los
  mismos estilos (encabezado verde, negrita blanca, bordes).

## Instalación y ejecución

```bash
cd agenda-service
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Conexión a BD (mismos valores por defecto que application.properties)
export DB_HOST=localhost DB_PORT=5432 DB_NAME=classify DB_USER=classify_app DB_PASSWORD=

python manage.py runserver 8081
```

El servicio queda en `http://localhost:8081`. No requiere migraciones:
el modelo está marcado `managed = False` y usa la tabla `agendas` existente.

## Para conmutar el frontend (cuando decidas hacerlo)

1. En `agenda/agenda.html` (Spring): cambiar el `action` del formulario a
   `http://localhost:8081/guardar-agenda`.
2. En `programacion/programacion.html`: apuntar el botón de exportar a
   `http://localhost:8081/programacion/exportarExcel`.
3. Desactivar (comentar) `AgendaController.java` para evitar duplicidad.
