package br.com.hacerfak.coreWMS.modules.portaria.domain;

public enum StatusAgendamento {
    AGENDADO,
    NA_PORTARIA, // Check-in feito
    NA_DOCA, // Operação iniciada
    FINALIZADO, // Saiu
    CANCELADO,
    NO_SHOW // Não compareceu
}