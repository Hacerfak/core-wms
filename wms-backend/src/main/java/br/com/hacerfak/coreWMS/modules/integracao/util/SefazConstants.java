package br.com.hacerfak.coreWMS.modules.integracao.util;

import java.util.HashMap;
import java.util.Map;

public class SefazConstants {
    public static final Map<String, String> URLS_SEFAZ = new HashMap<>();
    static {
        String svrs = "https://cad.svrs.rs.gov.br/ws/cadconsultacadastro/cadconsultacadastro4.asmx";
        URLS_SEFAZ.put("RS", svrs);
        URLS_SEFAZ.put("SC", svrs);
        URLS_SEFAZ.put("RJ", svrs);
        URLS_SEFAZ.put("ES", svrs);
        URLS_SEFAZ.put("AC", svrs);
        URLS_SEFAZ.put("RN", svrs);
        URLS_SEFAZ.put("PB", svrs);
        URLS_SEFAZ.put("DF", svrs);
        URLS_SEFAZ.put("TO", svrs);
        URLS_SEFAZ.put("CE", svrs);
        URLS_SEFAZ.put("AL", svrs);
        URLS_SEFAZ.put("AP", svrs);
        URLS_SEFAZ.put("PA", svrs);
        URLS_SEFAZ.put("PI", svrs);
        URLS_SEFAZ.put("RO", svrs);
        URLS_SEFAZ.put("RR", svrs);
        URLS_SEFAZ.put("SE", svrs);

        // Específicos
        URLS_SEFAZ.put("SP", "https://nfe.fazenda.sp.gov.br/ws/cadconsultacadastro4.asmx");
        URLS_SEFAZ.put("PR", "https://nfe.sefa.pr.gov.br/nfe/CadConsultaCadastro4");
        URLS_SEFAZ.put("MG", "https://nfe.fazenda.mg.gov.br/nfe2/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("BA", "https://nfe.sefaz.ba.gov.br/webservices/CadConsultaCadastro4/CadConsultaCadastro4.asmx");
        URLS_SEFAZ.put("GO", "https://nfe.sefaz.go.gov.br/nfe/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("PE", "https://nfe.sefaz.pe.gov.br/nfe-service/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("MT", "https://nfe.sefaz.mt.gov.br/nfews/v2/services/CadConsultaCadastro4");
        URLS_SEFAZ.put("MS", "https://nfe.sefaz.ms.gov.br/ws/CadConsultaCadastro4");
        URLS_SEFAZ.put("AM", "https://nfe.sefaz.am.gov.br/services2/services/CadConsultaCadastro4");
    }

    public static final Map<String, String> MAPEAMENTO_CRT = new HashMap<>();
    static {
        // --- CRT 1: SIMPLES NACIONAL ---
        MAPEAMENTO_CRT.put("SIMPLES NACIONAL", "1");
        MAPEAMENTO_CRT.put("EMPRESA OPTANTE PELO SIMPLES NACIONAL", "1");
        MAPEAMENTO_CRT.put("MICRO EPP/SIMPLES NACIONAL", "1");
        MAPEAMENTO_CRT.put("SIMPLES NACIONAL - SIMPLES NACIONAL", "1");
        MAPEAMENTO_CRT.put("SIMPLES NACIONAL - SUBSTITUTO TRIBUTARIO", "1");
        MAPEAMENTO_CRT.put("MICROEMPRESA - MICROEMPRESA SEM FAIXA", "1");
        MAPEAMENTO_CRT.put("MICROEMPRESA - ESTABELECIMENTO UNICO CENTRALIZADOR", "1");

        // --- CRT 2: SIMPLES NACIONAL (EXCESSO DE SUBLIMITE) ---
        // Conforme seu CSV, Faixa A e B estão mapeadas como 2
        MAPEAMENTO_CRT.put("MICROEMPRESA - SIMPLES FAIXA A", "2");
        MAPEAMENTO_CRT.put("MICROEMPRESA - SIMPLES FAIXA B", "2");
        MAPEAMENTO_CRT.put("MICROEMPRESA - SIMPLES FAIXA B COM ISS", "2");
        MAPEAMENTO_CRT.put("MICROEMPRESA - SIMPLES FAIXA C COM ISS", "2");

        // --- CRT 4: MEI ---
        MAPEAMENTO_CRT.put("SIMPLES NACIONAL - MEI", "4");
        MAPEAMENTO_CRT.put("SIMEI", "4");
        MAPEAMENTO_CRT.put("EMPRESA OPTANTE SIMEI", "4"); // Variação comum
        MAPEAMENTO_CRT.put("EMPRESA OPTANTE SIMEI (NORMALMENTE UTILIZADO PELAS EMPRESAS MEI)", "4");
        MAPEAMENTO_CRT.put("SIMPLES NACIONAL/SIMEI", "4");

        // --- CRT 3: REGIME NORMAL (E OUTROS PADRÕES) ---
        // Muitos textos variados caem aqui
        MAPEAMENTO_CRT.put("NORMAL", "3");
        MAPEAMENTO_CRT.put("NORMAL - NORMAL", "3");
        MAPEAMENTO_CRT.put("REGIME DE TRIBUTACAO NORMAL", "3");
        MAPEAMENTO_CRT.put("NORMAL - REGIME PERIÓDICO DE APURAÇÃO", "3");
        MAPEAMENTO_CRT.put("REGIME DE TRIBUTACAO NORMAL", "3");
        MAPEAMENTO_CRT.put("NORMAL - CENTRALIZADO", "3");
        MAPEAMENTO_CRT.put("NORMAL - CENTRALIZADOR", "3");
        MAPEAMENTO_CRT.put("NORMAL - DILACAO DE PRAZO SEM RETORNO R.S.", "3");

        // Regimes Diferenciados geralmente são Normal para fins de NFe (destacam
        // imposto)
        MAPEAMENTO_CRT.put("REGIME DIFERENCIADO - SUBSTITUTO TRIBUTARIO", "3");
        MAPEAMENTO_CRT.put("REGIME DIFERENCIADO - FORNECIMENTO DE ALIMENTACAO", "3");
        MAPEAMENTO_CRT.put("REGIME DIFERENCIADO - BOM EMPREGO", "3");
        MAPEAMENTO_CRT.put("REGIME DIFERENCIADO - MAIS EMPREGO", "3");
        MAPEAMENTO_CRT.put("REGIME DIFERENCIADO - PARANA COMPETITIVO", "3");

        // Recuperação Judicial
        MAPEAMENTO_CRT.put("RECUPERACAO JUDICIAL - NORMAL", "3");
        MAPEAMENTO_CRT.put("RECUPERACAO JUDICIAL - CENTRALIZADOR", "3");
        MAPEAMENTO_CRT.put("RECUPERACAO JUDICIAL - CENTRALIZADO", "3");
        MAPEAMENTO_CRT.put("RECUPERACAO JUDICIAL - SUBSTITUTO TRIBUTARIO", "3");
    }
}