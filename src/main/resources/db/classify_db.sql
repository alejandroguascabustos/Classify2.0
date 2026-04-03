-- CLASSIFY - BASE DE DATOS UNIFICADA (PostgreSQL)
-- Este archivo centraliza la estructura, usuario y datos básicos del sistema.

-- 1. ESTRUCTURA DE LA TABLA (Requerida para el funcionamiento de Classify)
-- =========================================================================

CREATE TABLE IF NOT EXISTS registro_usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    apellido VARCHAR(120) NOT NULL,
    correo VARCHAR(180) NOT NULL UNIQUE,
    documento VARCHAR(40) NOT NULL UNIQUE,
    telefono VARCHAR(30) NOT NULL,
    nombre_usuario VARCHAR(100) NOT NULL UNIQUE,
    pass_hash VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(20) NOT NULL,
    curso VARCHAR(50),
    materia VARCHAR(150),
    nombre_estudiante VARCHAR(150),
    codigo_docente_asignado VARCHAR(20) UNIQUE,
    codigo_docente_referencia VARCHAR(20),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices de optimización
CREATE INDEX IF NOT EXISTS idx_registro_usuarios_tipo ON registro_usuarios (tipo_usuario);
CREATE INDEX IF NOT EXISTS idx_registro_usuarios_codigo_ref ON registro_usuarios (codigo_docente_referencia);

-- 2. DATOS DE PRUEBA / INICIALES

INSERT INTO registro_usuarios (
    nombre, apellido, correo, documento, telefono, nombre_usuario, pass_hash,
    tipo_usuario, curso, materia, nombre_estudiante, codigo_docente_asignado,
    codigo_docente_referencia, creado_en
)
VALUES
    ('Admin', 'Test', 'admin@classify.local', 'TEST-ADM-BASE', '3000000001', 'admin', '$2y$10$IQMkN2GaYiKq908CK0aju.Z9GFBe981aHlGhI7aDCnu3CtN8UQjLK', 'administrador', NULL, NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP),
    ('Docente', 'Test', 'docente@classify.local', 'TEST-DOC-BASE', '3000000002', 'docente', '$2y$10$IQMkN2GaYiKq908CK0aju.Z9GFBe981aHlGhI7aDCnu3CtN8UQjLK', 'docente', NULL, 'Ciencias Naturales', NULL, 'DOC-BASE01', NULL, CURRENT_TIMESTAMP),
    ('Estudiante', 'Test', 'estudiante@classify.local', 'TEST-EST-BASE', '3000000003', 'estudiante', '$2y$10$IQMkN2GaYiKq908CK0aju.Z9GFBe981aHlGhI7aDCnu3CtN8UQjLK', 'estudiante', '7A', NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP),
    ('Acudiente', 'Test', 'acudiente@classify.local', 'TEST-ACU-BASE', '3000000004', 'acudiente', '$2y$10$IQMkN2GaYiKq908CK0aju.Z9GFBe981aHlGhI7aDCnu3CtN8UQjLK', 'acudiente', NULL, NULL, 'Estudiante Demo', NULL, NULL, CURRENT_TIMESTAMP)
ON CONFLICT (nombre_usuario)
DO UPDATE SET
    nombre = EXCLUDED.nombre,
    apellido = EXCLUDED.apellido,
    correo = EXCLUDED.correo,
    documento = EXCLUDED.documento,
    telefono = EXCLUDED.telefono,
    pass_hash = EXCLUDED.pass_hash,
    tipo_usuario = EXCLUDED.tipo_usuario,
    curso = EXCLUDED.curso,
    materia = EXCLUDED.materia,
    nombre_estudiante = EXCLUDED.nombre_estudiante,
    codigo_docente_asignado = EXCLUDED.codigo_docente_asignado,
    codigo_docente_referencia = EXCLUDED.codigo_docente_referencia;

-- 3. CONFIGURACION PARA ADMINISTRADORES (OPCIONAL EN PSQL)
-- =========================================================================
-- Si necesitas registrar el usuario o la base en tu entorno Postgres manual:
/*
-- Crear Rol (psql):
-- SELECT 'CREATE ROLE classify_app LOGIN PASSWORD ''ClassifyLocal2026!''' WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'classify_app')\gexec
-- Crear DB (psql):
-- SELECT 'CREATE DATABASE classify OWNER classify_app' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'classify')\gexec
*/

-- ============================================================
-- 4. Tabla noticias para Classify - PostgreSQL
-- ============================================================

CREATE TABLE IF NOT EXISTS noticias (
    id_noticia       BIGSERIAL    PRIMARY KEY,
    titulo_noticia   VARCHAR(255) NOT NULL,
    autor_noticia    VARCHAR(120) NOT NULL,
    fecha_noticia    TIMESTAMP        NOT NULL,
    contenido_noticia TEXT        NOT NULL,
    tipo_noticia     VARCHAR(100),
    imagen_noticia   VARCHAR(500),
    creado_en        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para búsquedas frecuentes
CREATE INDEX IF NOT EXISTS idx_noticias_fecha
    ON noticias (fecha_noticia DESC);

CREATE INDEX IF NOT EXISTS idx_noticias_tipo
    ON noticias (tipo_noticia);

-- Datos de prueba (opcional, borrar en producción)
INSERT INTO noticias (titulo_noticia, autor_noticia, fecha_noticia, contenido_noticia, tipo_noticia)
VALUES
    ('Bienvenida al nuevo año escolar',
     'Administración',
     CURRENT_DATE,
     'Les damos la bienvenida al año escolar 2026. Este año contaremos con nuevas herramientas digitales para apoyar el aprendizaje de todos nuestros estudiantes.',
     'Académico'),
    ('Jornada deportiva institucional',
     'Coordinación',
     CURRENT_DATE - INTERVAL '2 days',
     'Este viernes se realizará la jornada deportiva institucional. Todos los estudiantes deben presentarse con ropa cómoda a las 7:00 AM.',
     'Deportivo')
ON CONFLICT DO NOTHING;

-- Permisos
-- GRANT ALL PRIVILEGES ON TABLE noticias TO classify_app;
-- GRANT USAGE, SELECT ON SEQUENCE noticias_id_noticia_seq TO classify_app;

-- ============================================================
-- 5. Tabla materiales para Classify - PostgreSQL
-- ============================================================

CREATE TABLE IF NOT EXISTS materiales (
    id_material    BIGSERIAL    PRIMARY KEY,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo   VARCHAR(500),
    fecha_subida   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_usuario     BIGINT       NOT NULL REFERENCES registro_usuarios(id)
);

CREATE INDEX IF NOT EXISTS idx_materiales_usuario
    ON materiales (id_usuario);

CREATE INDEX IF NOT EXISTS idx_materiales_fecha
    ON materiales (fecha_subida DESC);

-- GRANT ALL PRIVILEGES ON TABLE materiales TO classify_app;
-- GRANT USAGE, SELECT ON SEQUENCE materiales_id_material_seq TO classify_app;