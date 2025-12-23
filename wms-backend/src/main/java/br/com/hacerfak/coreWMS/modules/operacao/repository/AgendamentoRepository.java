package br.com.hacerfak.coreWMS.modules.operacao.repository;

import br.com.hacerfak.coreWMS.modules.operacao.domain.Agendamento;
import br.com.hacerfak.coreWMS.modules.operacao.domain.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Para o painel da portaria: Quem está para chegar hoje?
    List<Agendamento> findByDataPrevistaInicioBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByStatus(StatusAgendamento status);
}