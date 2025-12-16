# --- ESTÁGIO 1: BUILD (Compilação do React) ---
FROM node:20-alpine AS build

WORKDIR /app

# Copia os arquivos de dependência
COPY package.json package-lock.json ./

# Instala as dependências
RUN npm ci

# Copia o código fonte
COPY . .

# Argumento de Build para definir a URL da API (Opcional, bom para CI/CD)
# Se não passar nada, ele usa o que está no código ou .env
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Compila o projeto (Gera a pasta /dist)
RUN npm run build

# --- ESTÁGIO 2: PRODUCTION (Servidor Nginx) ---
FROM nginx:alpine

# Remove a configuração padrão do Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copia a nossa configuração customizada (para suportar React Router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia os arquivos compilados do estágio anterior para o Nginx
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]