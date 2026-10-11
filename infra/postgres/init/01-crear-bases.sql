-- Una base de datos por microservicio (Vista de Despliegue: auth_db, reservas_db).
-- Las tablas NO se crean aquí: cada servicio es dueño de su esquema mediante Flyway
-- (ms-auth/src/main/resources/db/migration).
CREATE DATABASE auth_db;
CREATE DATABASE reservas_db;
