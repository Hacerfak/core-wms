package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum TipoEstrutura {
    PORTA_PALLET, // Capacidade fixa (geralmente 1 pallet por endereço)
    BLOCADO, // Capacidade variável (depende do produto)
    DRIVE_IN, // Similar ao blocado, mas com estrutura
    PUSH_BACK
}