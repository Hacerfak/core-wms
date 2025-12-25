-- 1. ARMAZÉM PRINCIPAL
INSERT INTO tb_armazem (
        nome,
        codigo,
        endereco_completo,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Centro de Distribuição Matriz',
        'CD01',
        'Rodovia dos Bandeirantes, KM 100',
        TRUE,
        'SEED',
        NOW()
    );
-- 2. ÁREAS (ZONAS)
-- Recebimento (Doca)
INSERT INTO tb_area (
        nome,
        codigo,
        armazem_id,
        tipo,
        padrao_recebimento,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Doca de Recebimento',
        'DOC',
        (
            SELECT id
            FROM tb_armazem
            WHERE codigo = 'CD01'
        ),
        'DOCA',
        TRUE,
        TRUE,
        'SEED',
        NOW()
    );
-- Armazenagem (Estoque)
INSERT INTO tb_area (
        nome,
        codigo,
        armazem_id,
        tipo,
        padrao_recebimento,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Porta Pallet Rua A',
        'RUA-A',
        (
            SELECT id
            FROM tb_armazem
            WHERE codigo = 'CD01'
        ),
        'ARMAZENAGEM',
        FALSE,
        TRUE,
        'SEED',
        NOW()
    );
-- Expedição (Stage)
INSERT INTO tb_area (
        nome,
        codigo,
        armazem_id,
        tipo,
        padrao_expedicao,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Stage de Expedição',
        'EXP',
        (
            SELECT id
            FROM tb_armazem
            WHERE codigo = 'CD01'
        ),
        'STAGE',
        TRUE,
        TRUE,
        'SEED',
        NOW()
    );
-- 3. LOCALIZAÇÕES (ENDEREÇOS)
-- Doca Virtual
INSERT INTO tb_localizacao (
        codigo,
        endereco_completo,
        tipo,
        tipo_estrutura,
        area_id,
        virtual,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        '01',
        'CD01DOC01',
        'DOCA',
        'BLOCADO',
        (
            SELECT id
            FROM tb_area
            WHERE codigo = 'DOC'
        ),
        TRUE,
        TRUE,
        'SEED',
        NOW()
    );
-- Endereços de Rua (Físicos)
INSERT INTO tb_localizacao (
        codigo,
        endereco_completo,
        tipo,
        tipo_estrutura,
        area_id,
        virtual,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        '01-01-01',
        'CD01RUA-A01-01-01',
        'PULMAO',
        'PORTA_PALLET',
        (
            SELECT id
            FROM tb_area
            WHERE codigo = 'RUA-A'
        ),
        FALSE,
        TRUE,
        'SEED',
        NOW()
    ),
    (
        '01-01-02',
        'CD01RUA-A01-01-02',
        'PULMAO',
        'PORTA_PALLET',
        (
            SELECT id
            FROM tb_area
            WHERE codigo = 'RUA-A'
        ),
        FALSE,
        TRUE,
        'SEED',
        NOW()
    ),
    (
        '01-01-03',
        'CD01RUA-A01-01-03',
        'PULMAO',
        'PORTA_PALLET',
        (
            SELECT id
            FROM tb_area
            WHERE codigo = 'RUA-A'
        ),
        FALSE,
        TRUE,
        'SEED',
        NOW()
    ),
    (
        '01-02-01',
        'CD01RUA-A01-02-01',
        'PICKING',
        'PORTA_PALLET',
        (
            SELECT id
            FROM tb_area
            WHERE codigo = 'RUA-A'
        ),
        FALSE,
        TRUE,
        'SEED',
        NOW()
    );
-- 4. PARCEIROS
-- Depositante (Dono da mercadoria)
INSERT INTO tb_parceiro (
        nome,
        nome_fantasia,
        cpf_cnpj,
        tipo,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Industria de Bebidas LTDA',
        'Bebidas X',
        '12345678000100',
        'CLIENTE',
        TRUE,
        'SEED',
        NOW()
    );
-- Fornecedor
INSERT INTO tb_parceiro (
        nome,
        nome_fantasia,
        cpf_cnpj,
        tipo,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'Fornecedor de Embalagens SA',
        'PackMaster',
        '98765432000199',
        'FORNECEDOR',
        TRUE,
        'SEED',
        NOW()
    );
-- 5. PRODUTOS
INSERT INTO tb_produto (
        sku,
        nome,
        ean13,
        unidade_medida,
        peso_bruto_kg,
        depositante_id,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'SKU-001',
        'Coca-Cola 2L',
        '7891234567890',
        'UN',
        2.100,
        (
            SELECT id
            FROM tb_parceiro
            WHERE cpf_cnpj = '12345678000100'
        ),
        TRUE,
        'SEED',
        NOW()
    ),
    (
        'SKU-002',
        'Fanta Laranja 2L',
        '7891234567891',
        'UN',
        2.100,
        (
            SELECT id
            FROM tb_parceiro
            WHERE cpf_cnpj = '12345678000100'
        ),
        TRUE,
        'SEED',
        NOW()
    ),
    (
        'SKU-003',
        'Caixa Papelão P',
        '7899999999999',
        'UN',
        0.500,
        (
            SELECT id
            FROM tb_parceiro
            WHERE cpf_cnpj = '12345678000100'
        ),
        TRUE,
        'SEED',
        NOW()
    );
-- 6. IMPRESSORA DE TESTE
INSERT INTO tb_impressora (
        nome,
        descricao,
        tipo_conexao,
        endereco_ip,
        porta,
        ativo,
        criado_por,
        data_criacao
    )
VALUES (
        'IMP_TESTE',
        'Impressora Virtual (Netcat)',
        'REDE',
        '127.0.0.1',
        9100,
        TRUE,
        'SEED',
        NOW()
    );
-- 7. TEMPLATE ZPL (LPN Padrão)
INSERT INTO tb_etiqueta_template (
        nome,
        tipo_finalidade,
        largura_mm,
        altura_mm,
        padrao,
        zpl_codigo,
        criado_por,
        data_criacao
    )
VALUES (
        'Etiqueta LPN Padrão 100x100',
        'LPN',
        100,
        100,
        TRUE,
        '^XA
^FO50,50^ADN,36,20^FD{{DESC}}^FS
^FO50,100^ADN,18,10^FDSKU: {{SKU}}^FS
^FO50,150^ADN,18,10^FDLote: {{LOTE}} Val: {{VALIDADE}}^FS
^FO50,200^BY3
^BCN,100,Y,N,N
^FD{{LPN_CODIGO}}^FS
^XZ',
        'SEED',
        NOW()
    );