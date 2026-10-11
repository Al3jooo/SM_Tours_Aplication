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
FROM nginx:1.30.5-alpine
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/seguridad.conf /etc/nginx/samaria360/seguridad.conf
COPY --from=compilacion /fuente/dist /usr/share/nginx/html
EXPOSE 80
