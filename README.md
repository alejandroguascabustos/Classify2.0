# Proyecto Classify 📗

## Descripción del proyecto
**Classify** es un proyecto educativo en desarrollo creado para la administración y gestión de clases disponible para cualquier institución educativa como parte de su proceso de aprendizaje en desarrollo full stack.  
El objetivo del proyecto es construir una **plataforma escolar** para el colegio **Moralba Sur Oriental**, que permita gestionar agendamiento de clases.

El proyecto combina dos tecnologías principales:

- **Frontend y gestión de vistas con CodeIgniter 4 (PHP Framework)**
---

## Funcionalidades del sistema

### Módulo de usuarios

- **Registro de usuarios** 
- **Inicio de sesión** con validación de credenciales.  
- **Roles de usuario**:  
  - **Docente:** puede crear agendamientos, cargar materiales y gestionar clases.  
  - **Estudiante:** puede visualizar los materiales y los agendamientos asignados.  
  - **Administrador:** puede visualizar todos los servicios
  - **Coordinador:** puede visualizar y crear agendamientos
  - **Acudiente:** tiene acceso a ciertos modulos
  - **Estudiante:** tiene acceso a funcionalidades dadas para su formación
- **Cierre de sesión seguro.**

---

### Módulo de agendamientos

- **Creación de agendamientos:** la coordinación puede programar clases, reuniones o actividades académicas.  
- **Visualización de agendamientos:**  
  - Los estudiantes pueden ver sus clases agendadas.  
- **Edición y eliminación de agendamientos** (solo para usuario coordinador y administrador).

---

### Funcionalidades generales

- Diseño adaptable con HTML, CSS y JS.  
- Validaciones de formularios en frontend y backend.  
- Conexión entre el frontend vanilla y el backend en Java.  
- Acceso controlado según tipo de usuario.  
- Integración con base de datos **PostgreSQL**.  

---

## Despliegue local

1. Descargue el archivo .zip
2. Agregue a la carpeta htdocs ubicada comunmente en la ubicación C:
3. Abra Xampp, VisualStudioCode y abra el proyecto en la ubicación indicada en el punto 2.
4. Si por algun motivo aparece error en el despliegue elimine la carpeta 'Vendor' e instale nuevamente el composer estando en la ubicación del proyecto, usando la terminal.
5. Abra de nuevo e intente abrir la ubicación http://localhost/classify/public/

