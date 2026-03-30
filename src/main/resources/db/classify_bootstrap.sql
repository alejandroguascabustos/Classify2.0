\echo Configurando entorno local compartido de PostgreSQL para Classify...

SELECT 'CREATE ROLE classify_app LOGIN PASSWORD ''ClassifyLocal2026!'''
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_roles
    WHERE rolname = 'classify_app'
)\gexec

ALTER ROLE classify_app WITH LOGIN PASSWORD 'ClassifyLocal2026!';

SELECT 'CREATE DATABASE classify OWNER classify_app'
WHERE NOT EXISTS (
    SELECT 1
    FROM pg_database
    WHERE datname = 'classify'
)\gexec

GRANT ALL PRIVILEGES ON DATABASE classify TO classify_app;

\echo Base y usuario listos. Luego ejecuta:
\echo psql -d classify -f src/main/resources/db/registro_usuarios.sql
\echo psql -d classify -f src/main/resources/db/usuarios_test.sql
