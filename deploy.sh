#!/bin/bash

# Para o script se der erro em algum comando
set -e

echo "🚀 Iniciando Deploy Completo do WMS..."

# 1. Parar containers antigos
echo "🛑 Parando serviços..."
# O flag --remove-orphans limpa containers que não estão mais no arquivo yaml
docker compose -f docker-compose.prod.yml down --remove-orphans #-v

# 2. Reconstruir as imagens (Backend Java + Frontend React)
echo "🔨 Compilando e Construindo imagens (Isso pode demorar)..."
docker compose -f docker-compose.prod.yml build

# 3. Subir
echo "✅ Subindo WMS..."
docker compose -f docker-compose.prod.yml up -d

# 4. Limpeza
docker image prune -f

echo "🎉 DEPLOY SUCESSO!"
echo "Acesse no navegador: http://localhost (Porta 80)"