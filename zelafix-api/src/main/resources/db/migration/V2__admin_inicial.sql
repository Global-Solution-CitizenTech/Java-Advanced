-- =========================================================================
-- V2: Cadastros Base (Localidades, Categorias, Status) e ADMIN Inicial
-- =========================================================================

-- 1. ESTADO
INSERT INTO estado (nm_estado) VALUES ('São Paulo');

-- 2. CIDADE
INSERT INTO cidade (nm_cidade, estado_id_estado) VALUES ('São Paulo', 1);

-- 3. BAIRRO
INSERT INTO bairro (nm_bairro, cidade_id_cidade) VALUES ('Bela Vista', 1);
INSERT INTO bairro (nm_bairro, cidade_id_cidade) VALUES ('Pinheiros', 1);
INSERT INTO bairro (nm_bairro, cidade_id_cidade) VALUES ('Sé', 1);
INSERT INTO bairro (nm_bairro, cidade_id_cidade) VALUES ('Mooca', 1);
INSERT INTO bairro (nm_bairro, cidade_id_cidade) VALUES ('Santana', 1);

-- 4. CATEGORIA
INSERT INTO categoria (nm_categoria) VALUES ('ILUMINACAO_PUBLICA');
INSERT INTO categoria (nm_categoria) VALUES ('VIAS_E_PAVIMENTACAO');
INSERT INTO categoria (nm_categoria) VALUES ('DRENAGEM_E_ESGOTO');
INSERT INTO categoria (nm_categoria) VALUES ('LIMPEZA_URBANA');
INSERT INTO categoria (nm_categoria) VALUES ('ARVORES_E_PARQUES');
INSERT INTO categoria (nm_categoria) VALUES ('SINALIZACAO_E_TRANSITO');

-- 5. STATUS
INSERT INTO status (nm_status) VALUES ('PENDENTE');
INSERT INTO status (nm_status) VALUES ('EM_ANALISE');
INSERT INTO status (nm_status) VALUES ('EM_EXECUCAO');
INSERT INTO status (nm_status) VALUES ('CONCLUIDO');
INSERT INTO status (nm_status) VALUES ('REJEITADO');

-- 6. ENDERECO & USUARIO ADMIN (id_endereco = 1, id_usuario = 1)
INSERT INTO endereco (cep, rua, numero, referencia, bairro_id_bairro)
VALUES ('01311000', 'Avenida Paulista', '1000', 'Gabinete de Tecnologia', 1);

INSERT INTO usuario (nm_usuario, email, senha, cargo, endereco_id_endereco)
VALUES ('Administrador Geral', 'admin@zelafix.com.br', '${ADMIN_BCRYPT_HASH}', 'ADMIN', 1);

INSERT INTO telefone (num_ddd, num_tel, usuario_id_usuario)
VALUES ('11', '999990000', 1);

-- 7. PRIORIDADE
INSERT INTO prioridade (nm_prioridade) VALUES ('BAIXA');
INSERT INTO prioridade (nm_prioridade) VALUES ('MEDIA');
INSERT INTO prioridade (nm_prioridade) VALUES ('ALTA');
INSERT INTO prioridade (nm_prioridade) VALUES ('URGENTE');

-- 8. ORGAO_GOVERNO
INSERT INTO orgao_governo (orgao_governo) VALUES ('SECRETARIA_OBRAS');
INSERT INTO orgao_governo (orgao_governo) VALUES ('CET_TRANSITO');
INSERT INTO orgao_governo (orgao_governo) VALUES ('ILUMINACAO_PUBLICA');
INSERT INTO orgao_governo (orgao_governo) VALUES ('DEFESA_CIVIL');
INSERT INTO orgao_governo (orgao_governo) VALUES ('SUBPREFEITURA');
INSERT INTO orgao_governo (orgao_governo) VALUES ('SANEAMENTO_BASICO');
