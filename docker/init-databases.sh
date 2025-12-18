#!/bin/bash
set -e

# Cria os bancos de dados lógicos dentro do Container Principal
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    ALTER USER wms_user CREATEDB;
    CREATE DATABASE wms_master;
    
    GRANT ALL PRIVILEGES ON DATABASE wms_master TO wms_user;
EOSQL