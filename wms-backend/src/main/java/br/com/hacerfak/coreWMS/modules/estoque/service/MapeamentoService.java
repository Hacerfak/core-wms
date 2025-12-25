package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.*;
import br.com.hacerfak.coreWMS.modules.estoque.dto.*;
import br.com.hacerfak.coreWMS.modules.estoque.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapeamentoService {

    private final ArmazemRepository armazemRepository;
    private final AreaRepository areaRepository;
    private final LocalizacaoRepository localizacaoRepository;

    // =================================================================================
    // 1. ARMAZÉNS
    // =================================================================================

    public List<Armazem> listarArmazens() {
        return armazemRepository.findAll();
    }

    @Transactional
    public Armazem salvarArmazem(ArmazemRequest dto) {
        Armazem armazem;

        // --- LÓGICA DE EDIÇÃO ---
        if (dto.id() != null) {
            armazem = armazemRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Armazém não encontrado"));

            // Validação extra: Se mudou o código, verifica se o novo código já existe em
            // OUTRO armazém
            if (!armazem.getCodigo().equals(dto.codigo()) && armazemRepository.existsByCodigo(dto.codigo())) {
                throw new IllegalArgumentException("Já existe outro armazém com o código " + dto.codigo());
            }
        } else {
            // --- LÓGICA DE CRIAÇÃO ---
            if (armazemRepository.existsByCodigo(dto.codigo())) {
                throw new IllegalArgumentException("Já existe um armazém com o código " + dto.codigo());
            }
            armazem = new Armazem();
        }

        armazem.setCodigo(dto.codigo());
        armazem.setNome(dto.nome());
        armazem.setEnderecoCompleto(dto.enderecoCompleto());
        armazem.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        return armazemRepository.save(armazem);
    }

    @Transactional
    public void excluirArmazem(Long id) {
        if (!areaRepository.findByArmazemId(id).isEmpty()) {
            throw new IllegalStateException("Não é possível excluir armazém que possui áreas vinculadas.");
        }
        armazemRepository.deleteById(id);
    }

    // =================================================================================
    // 2. ÁREAS
    // =================================================================================

    public List<Area> listarAreas(Long armazemId) {
        return areaRepository.findByArmazemId(armazemId);
    }

    @Transactional
    public Area salvarArea(AreaRequest dto) {
        Armazem armazem = armazemRepository.findById(dto.armazemId())
                .orElseThrow(() -> new EntityNotFoundException("Armazém pai não encontrado"));

        Area area;
        boolean codigoMudou = false; // Flag para controlar a cascata

        if (dto.id() != null) {
            area = areaRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Área não encontrada"));

            // Verifica se o código está mudando (Ex: De 'DOC' para 'DOCA')
            if (!area.getCodigo().equals(dto.codigo())) {
                codigoMudou = true;
            }
        } else {
            area = new Area();
        }

        area.setArmazem(armazem);
        area.setCodigo(dto.codigo()); // Atualiza o código na memória
        area.setNome(dto.nome());
        area.setTipo(dto.tipo());
        area.setPadraoRecebimento(dto.padraoRecebimento() != null ? dto.padraoRecebimento() : false);
        area.setPadraoExpedicao(dto.padraoExpedicao() != null ? dto.padraoExpedicao() : false);
        area.setPadraoQuarentena(dto.padraoQuarentena() != null ? dto.padraoQuarentena() : false);
        area.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        Area areaSalva = areaRepository.save(area);

        // --- EFEITO CASCATA ---
        // Se o código da área mudou, precisamos atualizar o enderecoCompleto de TODAS
        // as posições filhas
        if (codigoMudou) {
            List<Localizacao> filhos = localizacaoRepository.findByAreaId(areaSalva.getId());
            for (Localizacao loc : filhos) {
                // Como 'loc' tem referência para 'area' (JPA), e 'area' foi atualizada acima,
                // chamar gerarEnderecoCompleto() vai pegar o novo código da área.
                loc.gerarEnderecoCompleto();
                localizacaoRepository.save(loc);
            }
        }

        return areaSalva;
    }

    @Transactional
    public void excluirArea(Long id) {
        if (!localizacaoRepository.findByAreaId(id).isEmpty()) {
            throw new IllegalStateException("Não é possível excluir área que possui endereços vinculados.");
        }
        areaRepository.deleteById(id);
    }

    // =================================================================================
    // 3. LOCALIZAÇÕES (CORRIGIDO PARA EDIÇÃO)
    // =================================================================================

    public List<Localizacao> listarLocais(Long areaId) {
        return localizacaoRepository.findByAreaId(areaId);
    }

    @Transactional
    public Localizacao salvarLocal(LocalizacaoRequest dto) {
        Area area = areaRepository.findById(dto.areaId())
                .orElseThrow(() -> new EntityNotFoundException("Área não encontrada"));

        Localizacao local;

        if (dto.id() != null) {
            local = localizacaoRepository.findById(dto.id())
                    .orElseThrow(() -> new EntityNotFoundException("Localização não encontrada"));
        } else {
            local = new Localizacao();
        }

        local.setArea(area);
        local.setCodigo(dto.codigo());
        local.setDescricao(dto.descricao());

        // Tipos
        local.setTipo(dto.tipo() != null ? dto.tipo() : area.getTipo());

        // --- CORREÇÃO AQUI: Salvando tipoEstrutura e capacidadeMaxima ---
        local.setTipoEstrutura(dto.tipoEstrutura() != null ? dto.tipoEstrutura() : TipoEstrutura.PORTA_PALLET);
        local.setCapacidadeMaxima(dto.capacidadeMaxima() != null ? dto.capacidadeMaxima() : 1);
        // ----------------------------------------------------------------

        // Regras
        local.setVirtual(dto.virtual() != null ? dto.virtual() : false);
        local.setPermiteMultiLpn(dto.permiteMultiLpn() != null ? dto.permiteMultiLpn() : true);

        // Capacidades
        local.setCapacidadeLpn(dto.capacidadeLpn() != null ? dto.capacidadeLpn() : 1);
        local.setCapacidadePesoKg(dto.capacidadePesoKg());

        // Status
        local.setBloqueado(dto.bloqueado() != null ? dto.bloqueado() : false);
        local.setAtivo(dto.ativo() != null ? dto.ativo() : true);

        return localizacaoRepository.save(local);
    }

    @Transactional
    public void excluirLocal(Long id) {
        // Futuro: Verificar saldo antes de deletar
        localizacaoRepository.deleteById(id);
    }

    public Localizacao buscarPorEnderecoCompleto(String endereco) {
        return localizacaoRepository.findByEnderecoCompleto(endereco)
                .orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado: " + endereco));
    }
}