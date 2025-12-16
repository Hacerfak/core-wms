#!/bin/bash
set -e

# Cria os bancos de dados lógicos dentro do Container Principal
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE wms_master;
    CREATE DATABASE wms_tenant_demo;
    
    GRANT ALL PRIVILEGES ON DATABASE wms_master TO wms_user;
    GRANT ALL PRIVILEGES ON DATABASE wms_tenant_demo TO wms_user;
EOSQL