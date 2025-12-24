package br.com.hacerfak.coreWMS.modules.faturamento.service;

import br.com.hacerfak.coreWMS.core.exception.EntityNotFoundException;
import br.com.hacerfak.coreWMS.modules.cadastro.domain.Parceiro;
import br.com.hacerfak.coreWMS.modules.cadastro.repository.ParceiroRepository;
import br.com.hacerfak.coreWMS.modules.faturamento.domain.*;
import br.com.hacerfak.coreWMS.modules.faturamento.dto.ExtratoCobrancaDTO;
import br.com.hacerfak.coreWMS.modules.faturamento.dto.ItemExtratoDTO;
import br.com.hacerfak.coreWMS.modules.faturamento.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FaturamentoService {

        private final AcordoPrecoRepository acordoRepository;
        private final ApontamentoServicoRepository apontamentoRepository;
        private final ServicoRepository servicoRepository;
        private final ParceiroRepository parceiroRepository;

        /**
         * Registra uma cobrança automática (Sistema -> Faturamento).
         */
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void registrarCobrancaAutomatica(Parceiro cliente, String codigoServico, BigDecimal quantidade,
                        String referencia) {
                if (cliente == null)
                        return;

                Optional<Servico> servicoOpt = servicoRepository.findByCodigo(codigoServico);
                if (servicoOpt.isEmpty())
                        return;
                Servico servico = servicoOpt.get();

                Optional<AcordoPreco> acordoOpt = acordoRepository.findByClienteIdAndServicoCodigo(cliente.getId(),
                                codigoServico);

                if (acordoOpt.isPresent()) {
                        AcordoPreco acordo = acordoOpt.get();
                        salvarApontamento(cliente, servico, quantidade, acordo.getPrecoUnitario(), referencia,
                                        "SISTEMA",
                                        "Cobrança Automática");
                }
        }

        /**
         * Apontamento Manual
         */
        @Transactional
        public ApontamentoServico apontamentoManual(Long clienteId, String codigoServico, BigDecimal quantidade,
                        String obs,
                        String usuario) {
                Parceiro cliente = parceiroRepository.findById(clienteId)
                                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

                Servico servico = servicoRepository.findByCodigo(codigoServico)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Serviço não encontrado: " + codigoServico));

                AcordoPreco acordo = acordoRepository.findByClienteIdAndServicoCodigo(clienteId, codigoServico)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Cliente não possui acordo de preço para o serviço: "
                                                                + servico.getNome()));

                return salvarApontamento(cliente, servico, quantidade, acordo.getPrecoUnitario(), "MANUAL", usuario,
                                obs);
        }

        private ApontamentoServico salvarApontamento(Parceiro cliente, Servico servico, BigDecimal qtd,
                        BigDecimal precoUnitario, String ref, String user, String obs) {
                BigDecimal total = precoUnitario.multiply(qtd);

                ApontamentoServico apontamento = ApontamentoServico.builder()
                                .cliente(cliente)
                                .servico(servico)
                                .dataReferencia(LocalDate.now())
                                .quantidade(qtd)
                                .valorUnitario(precoUnitario)
                                .valorTotal(total)
                                .origemReferencia(ref)
                                .usuarioApontamento(user)
                                .observacao(obs)
                                .faturado(false)
                                .build();

                return apontamentoRepository.save(apontamento);
        }

        // --- NOVO: GERAÇÃO DE EXTRATO ---

        @Transactional(readOnly = true)
        public ExtratoCobrancaDTO gerarExtratoPeriodo(Long clienteId, LocalDate inicio, LocalDate fim) {
                Parceiro cliente = parceiroRepository.findById(clienteId)
                                .orElseThrow(() -> new EntityNotFoundException("Cliente não encontrado"));

                // Busca todos os apontamentos do período
                List<ApontamentoServico> apontamentos = apontamentoRepository.findByClienteIdAndDataReferenciaBetween(
                                clienteId,
                                inicio, fim);

                // Transforma em DTOs de item
                List<ItemExtratoDTO> itens = apontamentos.stream()
                                .map(a -> new ItemExtratoDTO(
                                                a.getDataReferencia(),
                                                a.getServico().getNome(),
                                                a.getServico().getUnidadeMedida(),
                                                a.getQuantidade(),
                                                a.getValorUnitario(),
                                                a.getValorTotal(),
                                                a.getOrigemReferencia()))
                                .collect(Collectors.toList());

                // Calcula total geral
                BigDecimal totalGeral = apontamentos.stream()
                                .map(ApontamentoServico::getValorTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new ExtratoCobrancaDTO(
                                cliente.getNome(),
                                cliente.getCpfCnpj(),
                                inicio,
                                fim,
                                totalGeral,
                                itens);
        }

        /**
         * Método novo para processar cobranças quando uma entrada é finalizada.
         * Chamado via RabbitMQ (Assíncrono).
         */
        @Transactional
        public void calcularServicosEntrada(Long solicitacaoId) {
                // log.info("Processando faturamento para Solicitação Entrada ID: {}",
                // solicitacaoId);

                // AQUI entraria sua lógica de cálculo:
                // 1. Buscar a solicitação (Repository)
                // 2. Verificar contrato/acordo de preço da empresa
                // 3. Gerar registros na tabela de ApontamentoServico ou Fatura

                // Exemplo fictício (implemente conforme sua regra):
                // SolicitacaoEntrada sol =
                // solicitacaoEntradaRepository.findById(solicitacaoId)...
                // if (sol.getEmpresa().temContratoAtivo()) { ... }

                // log.info("Faturamento processado com sucesso (Placeholder).");
        }
}