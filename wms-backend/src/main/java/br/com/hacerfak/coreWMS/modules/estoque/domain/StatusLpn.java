package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum StatusLpn {
    EM_MONTAGEM, // Na doca, sendo bipado
    FECHADO, // Pronto para armazenar (Stage)
    ARMAZENADO, // Já no pulmão/blocado
    CONSUMIDO, // Aberto para picking (parcialmente vazio)
    EXPEDIDO // Saiu no caminhão
}