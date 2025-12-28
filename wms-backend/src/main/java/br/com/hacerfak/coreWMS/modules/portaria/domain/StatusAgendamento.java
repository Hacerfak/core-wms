package br.com.hacerfak.coreWMS.modules.portaria.domain;

public enum StatusAgendamento {
    AGENDADO,
    NA_PORTARIA, // Check-in feito
    NA_DOCA, // Operação iniciada
    AGUARDANDO_SAIDA, // Operação finalizada, doca liberada, aguardando portaria
    FINALIZADO, // Saiu
    CANCELADO,
    NO_SHOW // Não compareceu
}