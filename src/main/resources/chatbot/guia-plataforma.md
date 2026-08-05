# Guía de la plataforma Classify (base de conocimiento del asistente)

Este documento es la fuente de verdad que el Asistente Classify usa para responder.
Cualquiera del equipo puede editarlo: el servicio lo carga al arrancar la aplicación
(`ChatbotService`), así que tras cambiarlo hay que desplegar o reiniciar. Escribe
hechos verificables y en español; el asistente NO debe conocer nada que no esté
aquí o en la agenda que se le inyecta aparte.

## Qué es Classify

Classify (https://classify.in.net) es la plataforma de gestión educativa del
Colegio Moralba Sur Oriental, en Bogotá, Colombia. Centraliza el agendamiento de
clases, la cartelera de noticias, los materiales educativos y la gestión de
usuarios de la comunidad del colegio.

## Roles de usuario

- **Estudiante**: consulta clases, noticias y materiales; descarga contenido de Aprende.
- **Docente**: además agenda clases, publica noticias y carga materiales.
- **Acudiente**: consulta la información del estudiante a su cargo.
- **Coordinador**: además gestiona registros de usuarios.
- **Administrador**: acceso total, incluida la gestión de permisos por rol.

Lo que cada usuario ve en el menú lateral depende de los permisos que el
administrador configure; si alguien no ve un módulo, su rol no lo tiene habilitado.

## Acceso y cuentas

- **Iniciar sesión**: en /login, con el nombre de usuario o el correo registrado y la contraseña.
- **Recuperar contraseña**: en la pantalla de inicio de sesión, clic en
  "¿Olvidaste tu contraseña?". Llega un correo con una contraseña temporal; al
  entrar con ella, la plataforma obliga a definir una nueva. El enlace/contraseña
  temporal caduca a los 15 minutos: si expiró, se repite el proceso.
- **Registro**: desde /registro. Según la configuración vigente, el registro puede
  ser solo por invitación: en ese caso se necesita un enlace con token que envía
  el administrador, y sin él hay que solicitar acceso al colegio.
  - Los **docentes** reciben al registrarse un código único en su correo
    institucional. Ese código sirve para que estudiantes y acudientes se vinculen
    con el docente al registrarse.
  - Tras el registro puede requerirse **activar la cuenta** desde el enlace que
    llega por correo (/activar).
- **Cerrar sesión**: icono de salida en la esquina superior derecha.

## Módulos de la plataforma (menú lateral)

### Inicio (/menu)
Pantalla principal con los accesos a los módulos permitidos para el usuario.

### Agendar clase (/agenda)
Los docentes registran una clase con: grado y grupo (curso), materia, fecha, hora
de inicio, duración (30 a 120 minutos), modalidad (presencial o virtual), tema
principal, objetivos y recursos necesarios. El sistema valida conflictos y no
deja guardar si el salón (grado + grupo) ya tiene una clase en ese horario, o si
el profesor ya tiene clase en otro salón a la misma hora; en ese caso muestra con
quién y a qué hora choca.

### Clases agendadas (/clases-agendadas)
Vista de consulta de todas las clases registradas, con filtros por curso,
profesor y materia (listas desplegables) y tabla paginada. El resultado filtrado
se puede descargar en **Excel (.xlsx)** o **PDF**, ambos con el logo y los
colores del colegio. Incluye además un **dashboard**
(/clases-agendadas/dashboard) con gráficos de barras y de torta, agrupables por
curso, profesor o materia, e indicadores de totales.
(El antiguo módulo "Programación" ya no existe: fue reemplazado por esta vista.
Si alguien pregunta por él, indícale que ahora todo está en Clases agendadas.)

En "Agendar clase" (/agenda) también hay **cargue masivo**: se descarga una
plantilla de Excel, se diligencia (una clase por fila) y se sube; las filas con
conflicto de horario se reportan sin afectar las demás. Máximo 200 clases por
archivo.

### Noticias (/noticias)
Cartelera informativa del colegio. Las noticias tienen título, autor, fecha, tipo
(por ejemplo "Académico"), contenido e imagen opcional. Se pueden filtrar por tipo
y rango de fechas, y descargar en PDF (toda la cartelera filtrada o una noticia
individual). Los docentes y administradores pueden publicar.

### Gestión de registros (/gestion-registros)
Para coordinadores y administradores: administración de los usuarios registrados.
Incluye la **carga masiva por plantilla de Excel**: se descarga la plantilla, se
llenan las filas y se sube el archivo. Reglas de la plantilla: no cambiar la fila
de encabezados; tipos válidos: estudiante, docente, acudiente, coordinador; el
curso se escribe como grado + grupo (ej. "5B"); documento de 8 a 11 dígitos;
teléfono de 7 a 10 dígitos; correo válido. Si una fila tiene errores, la carga
reporta la fila y el motivo.

### Aprende (/aprende)
Materiales de estudio y recursos organizados para los estudiantes.

### Contacta a un profe (/contacta)
Formulario para enviarle un mensaje directo a un docente. La respuesta llega al
correo del remitente.

### Mis Materiales (/mismateriales) y Carga Materiales (/materiales)
Descarga y subida de material educativo (documentos, presentaciones, etc.).
El tamaño máximo por archivo es **10 MB**.

### Gestión de permisos (/gestion-permisos)
Exclusivo del administrador: define qué módulos ve cada rol y permite
excepciones por usuario (permitir o bloquear un módulo puntual).

## Asistente y soporte

- Este chat responde dudas de la plataforma y consultas sobre las clases
  agendadas de los próximos 7 días.
- Para hablar con una persona: el propio chat ofrece el formulario de contacto
  cuando hace falta; también existen la página /soporte y el formulario público
  de /contacto.

## Cosas que NO están disponibles (no ofrecerlas)

- Calificar profesores, ver notas o consultar izadas de bandera: esas secciones
  no están habilitadas actualmente en la plataforma.
- Cambiar el correo o los datos personales desde la propia cuenta: hoy eso lo
  gestiona el colegio (canal de soporte).
- La plataforma no envía recordatorios automáticos de clases (por ahora).

## Preguntas frecuentes

- **¿Olvidé mi usuario?** El usuario es el nombre de usuario elegido al
  registrarse, pero también se puede iniciar sesión con el correo.
- **¿Por qué no veo un módulo en el menú?** El rol no lo tiene permitido; lo
  habilita el administrador en Gestión de permisos.
- **¿Por qué no puedo agendar una clase?** Casi siempre es un conflicto de
  horario: el salón o el profesor ya tienen clase a esa hora. El mensaje de
  error indica el choque exacto.
- **¿Puedo subir un archivo de más de 10 MB?** No; hay que dividirlo o
  comprimirlo.
- **¿Cómo se agrega un usuario nuevo?** Individualmente desde /registro (con
  invitación si está activada) o masivamente con la plantilla de Excel en
  Gestión de registros.
