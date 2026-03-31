-- ============================================================
-- Tabla noticias para Classify - PostgreSQL
-- Ejecutar: psql -d classify -U classify_app -f noticias.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS noticias (
    id_noticia       BIGSERIAL    PRIMARY KEY,
    titulo_noticia   VARCHAR(255) NOT NULL,
    autor_noticia    VARCHAR(120) NOT NULL,
    fecha_noticia    DATETIME         NOT NULL,
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
GRANT ALL PRIVILEGES ON TABLE noticias TO classify_app;
GRANT USAGE, SELECT ON SEQUENCE noticias_id_noticia_seq TO classify_app;
