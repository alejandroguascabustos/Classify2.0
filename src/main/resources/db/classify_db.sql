-- 1. ESTRUCTURA DE LA TABLA (Requerida para el funcionamiento de Classify)

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
    curso VARCHAR(255),
    materia VARCHAR(255),
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

/*
-- Crear Rol (psql):
-- SELECT 'CREATE ROLE classify_app LOGIN PASSWORD ''ClassifyLocal2026!''' WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'classify_app')\gexec
-- Crear DB (psql):
-- SELECT 'CREATE DATABASE classify OWNER classify_app' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'classify')\gexec
*/


-- ============================================================
-- Módulo Gestión de Registros
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios_pendientes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    apellido VARCHAR(120) NOT NULL,
    correo VARCHAR(180) NOT NULL UNIQUE,
    documento VARCHAR(40) NOT NULL UNIQUE,
    telefono VARCHAR(30),
    nombre_usuario VARCHAR(100) NOT NULL UNIQUE,
    tipo_usuario VARCHAR(20) NOT NULL,
    curso VARCHAR(255),
    materia VARCHAR(255),
    nombre_estudiante VARCHAR(150),
    grado VARCHAR(20),
    grupo VARCHAR(20),
    estado VARCHAR(20) NOT NULL DEFAULT 'autorizado',
    origen VARCHAR(20) NOT NULL DEFAULT 'excel',
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_usuarios_pendientes_estado ON usuarios_pendientes (estado);

CREATE TABLE IF NOT EXISTS registro_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    correo VARCHAR(180) NOT NULL,
    usuario_pendiente_id BIGINT REFERENCES usuarios_pendientes(id) ON DELETE SET NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    intentos INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_registro_tokens_hash ON registro_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_registro_tokens_estado ON registro_tokens (estado);

-- Parámetros del colegio (una sola fila, id=1): rigen el formulario de registro
-- y la validación de la carga por Excel.
CREATE TABLE IF NOT EXISTS parametros_colegio (
    id INT PRIMARY KEY,
    num_grados INT NOT NULL DEFAULT 11,
    num_grupos INT NOT NULL DEFAULT 4,
    materias TEXT NOT NULL DEFAULT 'Matematicas,Español,Sociales,Historia,Ingles,Etica y valores,Educación fisica,Informatica,Ciencias Politicas,Religion',
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_por VARCHAR(100),
    CONSTRAINT parametros_unica_fila CHECK (id = 1)
);

-- Flujo de bienvenida / activación de cuenta
ALTER TABLE parametros_colegio ADD COLUMN IF NOT EXISTS nombre_colegio VARCHAR(150) NOT NULL DEFAULT 'Colegio Moralba Sur Oriental';
ALTER TABLE registro_usuarios ADD COLUMN IF NOT EXISTS debe_cambiar_password BOOLEAN NOT NULL DEFAULT false;

-- Un docente puede guardar varias materias y varios cursos (lista separada por comas):
-- se amplía el ancho de estas columnas en bases ya existentes.
ALTER TABLE usuarios_pendientes ALTER COLUMN curso TYPE VARCHAR(255);
ALTER TABLE usuarios_pendientes ALTER COLUMN materia TYPE VARCHAR(255);
ALTER TABLE registro_usuarios ALTER COLUMN curso TYPE VARCHAR(255);
ALTER TABLE registro_usuarios ALTER COLUMN materia TYPE VARCHAR(255);
CREATE TABLE IF NOT EXISTS activacion_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL REFERENCES registro_usuarios(id) ON DELETE CASCADE,
    estado VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    intentos INT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en TIMESTAMP NOT NULL,
    usado_en TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_activacion_tokens_hash ON activacion_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_activacion_tokens_usuario ON activacion_tokens (usuario_id);
