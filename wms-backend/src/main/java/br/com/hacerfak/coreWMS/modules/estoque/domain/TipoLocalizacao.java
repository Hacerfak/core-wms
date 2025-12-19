package br.com.hacerfak.coreWMS.modules.estoque.domain;

public enum TipoLocalizacao {
    PICKING, // Local de fácil acesso para separar unidades (ex: prateleira baixa)
    PULMAO, // Local de reserva no alto (ex: porta-palete ou blocado alto)
    DOCA, // Local temporário de entrada/saída (Virtual)
    STAGE, // Área de conferência no chão
    AVARIA, // Quarentena para produtos quebrados
    PERDA, // Local virtual para baixar estoque sumido
    QUARENTENA, // Local para produtos em observação
    ARMAZENAGEM, // Estoque Geral (Blocado ou Porta-Pallet Geral)
    SEGREGACAO // Local para separar produtos específicos (ex: lotes especiais)
}
