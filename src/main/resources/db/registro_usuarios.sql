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

CREATE INDEX IF NOT EXISTS idx_registro_usuarios_tipo
ON registro_usuarios (tipo_usuario);

CREATE INDEX IF NOT EXISTS idx_registro_usuarios_codigo_ref
ON registro_usuarios (codigo_docente_referencia);
