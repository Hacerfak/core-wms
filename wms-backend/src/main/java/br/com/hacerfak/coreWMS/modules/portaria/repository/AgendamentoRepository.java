package br.com.hacerfak.coreWMS.modules.portaria.repository;

import br.com.hacerfak.coreWMS.modules.portaria.domain.Agendamento;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

        Optional<Agendamento> findByCodigoReserva(String codigoReserva);

        // CORREÇÃO: Trazendo relacionamentos para exibir no Card (Transportadora, Doca,
        // Solicitacoes)
        @EntityGraph(attributePaths = {
                        "transportadora", "doca", "motorista", "turno",
                        "solicitacaoEntrada", "solicitacaoEntrada.doca", "solicitacaoEntrada.fornecedor", // Para ver
                                                                                                          // quem é o
                                                                                                          // parceiro da
                                                                                                          // entrada
                        "solicitacaoSaida", "solicitacaoSaida.cliente" // Para ver o cliente da saída
        })
        List<Agendamento> findByDataPrevistaInicioBetweenOrderByDataPrevistaInicioAsc(LocalDateTime inicio,
                        LocalDateTime fim);

        // Quem está no pátio agora? (Check-in feito, Check-out não)
        // @EntityGraph(attributePaths = { "transportadora", "doca", "motorista" })
        @Query("SELECT a FROM Agendamento a WHERE a.status IN ('NA_PORTARIA', 'NA_DOCA', 'AGUARDANDO_SAIDA') ORDER BY a.dataChegada DESC")
        List<Agendamento> findVeiculosNoPatio();

        // Agendamentos pendentes de XML (para dashboard ou alertas)
        @Query("SELECT a FROM Agendamento a WHERE a.tipo = 'ENTRADA' AND a.xmlVinculado = false AND a.status = 'AGENDADO'")
        List<Agendamento> findPendentesXml();

        // Verifica se existe algum veículo com status NA_DOCA ocupando este local
        @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.doca.id = :docaId AND a.status = 'NA_DOCA'")
        boolean isDocaOcupada(@Param("docaId") Long docaId);

        // Busca o agendamento ativo vinculado a uma solicitação de entrada (para
        // liberar a doca ao finalizar)
        @Query("SELECT a FROM Agendamento a WHERE a.solicitacaoEntrada.id = :solicitacaoId AND a.status = 'NA_DOCA'")
        Optional<Agendamento> findBySolicitacaoEntradaEmDoca(@Param("solicitacaoId") Long solicitacaoId);

        // Busca o agendamento pela solicitação de entrada (usando convenção de
        // nomenclatura do Spring Data)
        Optional<Agendamento> findBySolicitacaoEntradaId(Long solicitacaoEntradaId);
}