package br.com.hacerfak.coreWMS.modules.auditoria.controller;

import br.com.hacerfak.coreWMS.core.multitenant.TenantContext;
import br.com.hacerfak.coreWMS.modules.auditoria.domain.AuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditController {

    private final MongoTemplate mongoTemplate;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDITORIA_VISUALIZAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLog>> listarLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String acao,
            @PageableDefault(sort = "dataHora", direction = Sort.Direction.DESC, size = 20) Pageable pageable) {

        // 1. CONSTRUÇÃO DOS FILTROS (Criteria)
        List<Criteria> criteriaList = new ArrayList<>();

        // Filtro de Tenant
        String currentTenant = TenantContext.getTenant();
        Criteria tenantCriteria = new Criteria().orOperator(
                Criteria.where("tenantId").is(currentTenant),
                Criteria.where("tenantId").is("wms_master"),
                Criteria.where("tenantId").is(TenantContext.DEFAULT_TENANT_ID));
        criteriaList.add(tenantCriteria);

        // Filtro de Data
        if (inicio != null && fim != null) {
            criteriaList.add(Criteria.where("dataHora").gte(inicio).lte(fim));
        } else if (inicio != null) {
            criteriaList.add(Criteria.where("dataHora").gte(inicio));
        }

        // Filtros Opcionais
        if (usuario != null && !usuario.isBlank()) {
            criteriaList.add(Criteria.where("usuario").regex(usuario, "i"));
        }
        if (entidade != null && !entidade.isBlank()) {
            criteriaList.add(Criteria.where("entidade").is(entidade));
        }
        if (acao != null && !acao.isBlank()) {
            criteriaList.add(Criteria.where("evento").is(acao));
        }

        // Consolida os filtros
        Criteria finalCriteria = new Criteria();
        if (!criteriaList.isEmpty()) {
            finalCriteria.andOperator(criteriaList.toArray(new Criteria[0]));
        }

        // 2. QUERY DE CONTAGEM (Total Real)
        // CRÍTICO: Criamos uma query nova SEM paginação (.with(pageable))
        Query countQuery = new Query(finalCriteria);
        long count = mongoTemplate.count(countQuery, AuditLog.class);

        // 3. QUERY DE LISTAGEM (Paginada)
        // Agora aplicamos a paginação apenas para buscar os dados
        Query listQuery = new Query(finalCriteria).with(pageable);
        List<AuditLog> list = mongoTemplate.find(listQuery, AuditLog.class);

        // O PageableExecutionUtils usa o 'count' real para calcular o totalPages
        return ResponseEntity.ok(PageableExecutionUtils.getPage(list, pageable, () -> count));
    }
}