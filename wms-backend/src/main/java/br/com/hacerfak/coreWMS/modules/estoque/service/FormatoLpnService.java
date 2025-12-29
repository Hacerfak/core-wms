package br.com.hacerfak.coreWMS.modules.estoque.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.estoque.domain.FormatoLpn;
import br.com.hacerfak.coreWMS.modules.estoque.repository.FormatoLpnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FormatoLpnService {

    private final FormatoLpnRepository repository;

    public List<FormatoLpn> listarTodos() {
        return repository.findAll();
    }

    public List<FormatoLpn> listarAtivos() {
        return repository.findByAtivoTrue();
    }

    public FormatoLpn buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Formato de LPN não encontrado."));
    }

    @Transactional
    public FormatoLpn salvar(FormatoLpn formato) {
        // 1. Validação de Duplicidade de Código
        if (formato.getId() == null) {
            if (repository.existsByCodigo(formato.getCodigo())) {
                throw new IllegalArgumentException("Já existe um formato com o código " + formato.getCodigo());
            }
        } else {
            if (repository.existsByCodigoAndIdNot(formato.getCodigo(), formato.getId())) {
                throw new IllegalArgumentException("Já existe outro formato com o código " + formato.getCodigo());
            }
        }

        // 2. Validação de Dimensões (Não podem ser negativas)
        validarDimensoes(formato);

        // 3. Defaults
        if (formato.getPesoSuportadoKg() == null)
            formato.setPesoSuportadoKg(BigDecimal.ZERO);
        if (formato.getTaraKg() == null)
            formato.setTaraKg(BigDecimal.ZERO);

        return repository.save(formato);
    }

    @Transactional
    public void alternarStatus(Long id) {
        FormatoLpn formato = buscarPorId(id);
        formato.setAtivo(!formato.isAtivo());
        repository.save(formato);
    }

    @Transactional
    public void excluir(Long id) {
        // Futuro: Verificar se existem LPNs usando este formato antes de excluir
        // Se houver, lançar erro ou apenas inativar
        repository.deleteById(id);
    }

    private void validarDimensoes(FormatoLpn f) {
        if (isNegative(f.getAlturaM()) || isNegative(f.getLarguraM()) || isNegative(f.getProfundidadeM())) {
            throw new IllegalArgumentException("Dimensões não podem ser negativas.");
        }
        if (isNegative(f.getPesoSuportadoKg()) || isNegative(f.getTaraKg())) {
            throw new IllegalArgumentException("Pesos não podem ser negativos.");
        }
    }

    private boolean isNegative(BigDecimal val) {
        return val != null && val.compareTo(BigDecimal.ZERO) < 0;
    }
}