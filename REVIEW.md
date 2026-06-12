# Revisión de código — rama `juan/integracion-revision`

> Documento de revisión. **No corrige nada automáticamente**: lista hallazgos y
> posibles ajustes. Los puntos marcados con 🔴 conviene atenderlos antes de
> producción. Los `// REVIEW:` inline en el código apuntan a estos mismos ítems.
>
> Última actualización: 2026-06-11

---

## 🔴 Seguridad

### 1. API key de Groq hardcodeada en el repositorio
`src/main/resources/application.properties:38`
```
groq.api.key=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
```
La clave quedó commiteada en texto plano (un commit previo decía "elimina Groq
API Key hardcodeada", pero sigue presente). Cualquiera con acceso al repo —o al
historial— la tiene.
**Ajuste sugerido:** moverla a variable de entorno y **rotar la clave** ya
expuesta:
```properties
groq.api.key=${GROQ_API_KEY:}
```

### 2. La contraseña de BD quedó vacía por defecto
`application.properties:29` → `spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:}`
Correcto que use variable de entorno, pero el default vacío solo debe valer en
local. En el servidor, exigir la variable (sin default).

### 3. `POST /guardar-agenda` sin autenticación ni validación
`AgendaController.java:27`
- No verifica sesión: cualquiera puede crear agendas con un POST directo.
- No hay `@Valid` sobre `Agenda`; entran datos sin validar (fechas, duración
  negativa, campos vacíos).
**Ajuste:** comprobar `session.getAttribute("usuarioId")` (como en otras vistas)
y anotar el parámetro con `@Valid` + restricciones en la entidad.

### 4. `@csrf_exempt` en el servicio Django
`agenda-service/agenda/views.py:102`
Aceptable como puente temporal (el form original no manda token de Django), pero
el endpoint queda abierto a CSRF. Si el módulo Python pasa a producción, habilitar
CSRF o validar origen/token compartido con Spring.

---

## 🟠 Robustez / manejo de errores

### 5. Fuga de detalle interno al cliente
`AgendaController.java:51`
```java
response.put("message", "Error al guardar: " + e.getMessage());
```
Devuelve el mensaje de la excepción al navegador (puede exponer detalle de BD).
Mejor: mensaje genérico al usuario y `log.error("...", e)` en el servidor.
Mismo patrón en `views.py:123`.

### 6. `catch (Exception e)` demasiado amplio
`AgendaController.java:47` y `views.py:96,120`
Captura todo sin registrar (`log`). Si algo falla, no queda rastro. Añadir
logging y, si se puede, capturar excepciones más específicas.

---

## 🟡 Rendimiento

### 7. Export a Excel carga todo en memoria sin límite
`AgendaController.java:64` / `views.py:137`
`listarAgendas()` trae **todas** las filas y `autoSizeColumn` recorre cada celda.
Con muchos registros se vuelve lento y consume memoria.
**Ajuste:** paginar o filtrar por rango de fechas; considerar streaming (SXSSF en
POI) para volúmenes grandes.

### 8. Logging SQL en modo desarrollo dentro de la config principal
`application.properties:51-55`
```
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```
`TRACE` del binder imprime **valores de parámetros** (incluye datos personales) y
ralentiza. Útil en local, peligroso/ruidoso en producción. Mover a un
`application-dev.properties` (perfil `dev`).

### 9. `spring.jpa.hibernate.ddl-auto=update` en config principal
`application.properties:50`
En producción `update` puede alterar el esquema de forma inesperada. Recomendado
`validate` en prod y manejar cambios con migraciones (Flyway/Liquibase).

---

## 🟢 Diseño / mantenibilidad

### 10. Duplicación de rutas Java ↔ Python
`AgendaController` (Java) y `views.py` (Python) exponen las **mismas** rutas
(`/guardar-agenda`, `/programacion/exportarExcel`). Si ambos servicios corren a la
vez sin un proxy que decida, hay riesgo de comportamiento duplicado. El
`PythonProxyController` (añadido en esta rama) centraliza el acceso bajo `/py/**`;
definir **una sola** fuente de verdad por endpoint.

### 11. `AgendaService` podría ser `@Transactional`
`AgendaService.java:17` — el `save` está bien, pero marcar el método de escritura
con `@Transactional` deja la intención explícita y agrupa operaciones futuras.

### 12. Construcción del Excel muy repetitiva
`AgendaController.java:100-109` — nueve líneas casi idénticas. Extraer un helper
`crearCelda(row, idx, valor, style)` reduce ruido y errores al añadir columnas.

---

## Notas sobre lo añadido en esta rama (auto-revisión)

- `StatusApiController` (`/api/*`): endpoints de solo lectura, sin estado. OK.
  Posible mejora: exponer también estado de conexión a BD reutilizando Actuator.
- `PythonProxyController` (`/py/**`): proxy simple con `HttpClient`. Mejoras
  posibles: (a) reenviar más cabeceras (cookies de sesión) si el módulo Python
  las necesitara; (b) hacer configurable el prefijo; (c) usar conexión keep-alive
  reutilizada (ya se reutiliza el `HttpClient`, bien).

---

### Leyenda de prioridad
🔴 crítico · 🟠 importante · 🟡 recomendable · 🟢 mejora opcional
