package br.com.hacerfak.printagent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class NetworkPrinterService {

    /**
     * Envia ZPL para impressora de rede (IP:Porta).
     * Ex: Zebra ligada no cabo de rede.
     */
    public void imprimirViaSocket(String ip, int porta, String zpl) throws IOException {
        log.info("Tentando conectar em {}:{}", ip, porta);

        // Abre conexão TCP direta (RAW 9100)
        try (Socket socket = new Socket(ip, porta);
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            byte[] dados = zpl.getBytes(StandardCharsets.UTF_8);
            out.write(dados);
            out.flush();
            log.info("ZPL enviado via Rede com sucesso ({} bytes)", dados.length);
        }
    }

    /**
     * Envia ZPL para compartilhamento Windows ou arquivo local.
     * Ex: \\SERVIDOR\Zebra01 ou /dev/usb/lp0
     */
    public void imprimirViaCompartilhamento(String caminho, String zpl) throws IOException {
        log.info("Tentando escrever em {}", caminho);

        try (FileOutputStream os = new FileOutputStream(caminho)) {
            os.write(zpl.getBytes(StandardCharsets.UTF_8));
            os.flush();
            log.info("ZPL enviado para Spooler/Arquivo com sucesso");
        }
    }
}