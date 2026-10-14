-- Samaria360 - servicio de un solo uso postgres-bases (docker-compose.yml).
-- Crea las bases de cada microservicio si no existen. A diferencia de init/01-crear-bases.sql, que solo corre con el
-- volumen vacío, se ejecuta en cada arranque: así un volumen creado antes de que existiera una base también la
-- recibe, sin borrar datos (nunca docker compose down -v). Es idempotente.
-- Las tablas NO se crean aquí: cada servicio es dueño de su esquema mediante Flyway.
SELECT 'CREATE DATABASE auth_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec

SELECT 'CREATE DATABASE reservas_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'reservas_db')\gexec

\echo 'Bases de datos al día: auth_db y reservas_db'
