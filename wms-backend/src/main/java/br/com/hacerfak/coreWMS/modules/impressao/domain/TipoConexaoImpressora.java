package br.com.hacerfak.coreWMS.modules.impressao.domain;

public enum TipoConexaoImpressora {
    REDE, // IP Direto (Socket 9100)
    USB_LOCAL, // Conectada na máquina do agente
    COMPARTILHAMENTO // Windows Share (\\Servidor\Imp)
}