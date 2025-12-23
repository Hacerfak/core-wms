package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum StatusQualidade {
    DISPONIVEL, // Bom para venda
    AVARIA, // Danificado
    VENCIDO, // Data de validade expirada
    BLOQUEADO, // Bloqueio administrativo/qualidade
    QUARENTENA // Aguardando inspeção
}