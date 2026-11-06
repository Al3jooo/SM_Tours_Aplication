# frontend — build de Vite servido por nginx (docker-compose.yml en la raíz).

# ---------- Compilación ----------
FROM node:24-alpine AS compilacion
WORKDIR /fuente

# Capa de dependencias: solo se reinstala si cambia el lockfile.
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---------- Ejecución ----------
# nginx sin root: la variante oficial unprivileged corre como el usuario nginx (UID 101) y escucha en 8080, porque un
# usuario sin privilegios no puede abrir puertos por debajo de 1024. Compose publica FRONTEND_PORT → 8080.
FROM nginxinc/nginx-unprivileged:1.31.7-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/seguridad.conf /etc/nginx/samaria360/seguridad.conf
COPY --from=compilacion /fuente/dist /usr/share/nginx/html
EXPOSE 8080
