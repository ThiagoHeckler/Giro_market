# Build: os tokens vêm de design/tokens.json, passado pelo compose como contexto adicional "design".
FROM node:24-alpine AS build
WORKDIR /app/web
COPY package.json package-lock.json ./
RUN npm ci
COPY --from=design . /app/design
COPY . .
RUN npm run build

FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx/default.conf.template /etc/nginx/templates/
COPY --from=build /app/web/dist /usr/share/nginx/html
EXPOSE 8080
