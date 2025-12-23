package br.com.hacerfak.coreWMS.modules.sistema.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.sistema.domain.Anexo;
import br.com.hacerfak.coreWMS.modules.sistema.repository.AnexoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnexoService {

    private final AnexoRepository anexoRepository;

    // Defina isso no application.yaml: app.upload.dir: /tmp/uploads
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public Anexo uploadArquivo(MultipartFile file, String entidadeTipo, Long entidadeId, String descricao) {
        try {
            // Cria diretório se não existir
            Path pathDir = Paths.get(uploadDir);
            if (!Files.exists(pathDir)) {
                Files.createDirectories(pathDir);
            }

            // Gera nome único para não sobrescrever
            String nomeOriginal = file.getOriginalFilename();
            String extensao = "";
            if (nomeOriginal != null && nomeOriginal.contains(".")) {
                extensao = nomeOriginal.substring(nomeOriginal.lastIndexOf("."));
            }
            String nomeArquivoFisico = UUID.randomUUID().toString() + extensao;

            Path targetLocation = pathDir.resolve(nomeArquivoFisico);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Salva metadados no banco
            Anexo anexo = Anexo.builder()
                    .nomeArquivo(nomeOriginal)
                    .contentType(file.getContentType())
                    .caminhoUrl(targetLocation.toString()) // Ou URL pública se fosse S3
                    .entidadeTipo(entidadeTipo)
                    .entidadeId(entidadeId)
                    .descricao(descricao)
                    .build();

            return anexoRepository.save(anexo);

        } catch (IOException ex) {
            throw new RuntimeException("Erro ao salvar arquivo " + file.getOriginalFilename(), ex);
        }
    }

    public byte[] recuperarArquivo(Long anexoId) {
        Anexo anexo = anexoRepository.findById(anexoId)
                .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado"));

        try {
            Path path = Paths.get(anexo.getCaminhoUrl());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo do disco", e);
        }
    }
}