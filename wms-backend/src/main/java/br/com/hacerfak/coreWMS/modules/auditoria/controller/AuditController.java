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
        Query query = new Query().with(pageable);
        List<Criteria> criteria = new ArrayList<>();

        // 1. Filtro de Segurança (Isolamento de Tenant)
        criteria.add(Criteria.where("tenantId").is(TenantContext.getTenant()));

        // 2. Filtro de Data (Range)
        if (inicio != null && fim != null) {
            criteria.add(Criteria.where("dataHora").gte(inicio).lte(fim));
        } else if (inicio != null) {
            criteria.add(Criteria.where("dataHora").gte(inicio));
        }

        // 3. Filtros Opcionais
        if (usuario != null && !usuario.isBlank()) {
            criteria.add(Criteria.where("usuario").regex(usuario, "i")); // Case insensitive
        }
        if (entidade != null && !entidade.isBlank()) {
            criteria.add(Criteria.where("entityName").is(entidade));
        }
        if (acao != null && !acao.isBlank()) {
            criteria.add(Criteria.where("action").is(acao));
        }

        // Monta a query final
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        // Executa com paginação otimizada
        List<AuditLog> list = mongoTemplate.find(query, AuditLog.class);

        // Count separado para paginação correta no Mongo
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AuditLog.class);

        return ResponseEntity.ok(PageableExecutionUtils.getPage(list, pageable, () -> count));
    }
}