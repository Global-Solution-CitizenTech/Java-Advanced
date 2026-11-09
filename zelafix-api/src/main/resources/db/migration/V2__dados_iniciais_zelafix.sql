-- =========================================================================
-- V2: Dados Iniciais de Semente (Seeds) do Ecossistema ZelaFix
-- Contextualizados na cidade de São Paulo (ODS 11 - Cidades Sustentáveis)
-- 100% Fiel a Database-Advanced/2TDSPG_2026_CodigoSql_ZelaFix.sql
-- =========================================================================

-- 1. ESTADO
INSERT INTO estado (id_estado, nm_estado) VALUES (1, 'São Paulo');
INSERT INTO estado (id_estado, nm_estado) VALUES (2, 'Rio de Janeiro');
INSERT INTO estado (id_estado, nm_estado) VALUES (3, 'Minas Gerais');
INSERT INTO estado (id_estado, nm_estado) VALUES (4, 'Paraná');
INSERT INTO estado (id_estado, nm_estado) VALUES (5, 'Rio Grande do Sul');

-- 2. CIDADE
INSERT INTO cidade (id_cidade, nm_cidade, estado_id_estado) VALUES (1, 'São Paulo', 1);
INSERT INTO cidade (id_cidade, nm_cidade, estado_id_estado) VALUES (2, 'Campinas', 1);
INSERT INTO cidade (id_cidade, nm_cidade, estado_id_estado) VALUES (3, 'Santos', 1);
INSERT INTO cidade (id_cidade, nm_cidade, estado_id_estado) VALUES (4, 'Rio de Janeiro', 2);
INSERT INTO cidade (id_cidade, nm_cidade, estado_id_estado) VALUES (5, 'Belo Horizonte', 3);

-- 3. BAIRRO
INSERT INTO bairro (id_bairro, nm_bairro, cidade_id_cidade) VALUES (1, 'Bela Vista', 1);
INSERT INTO bairro (id_bairro, nm_bairro, cidade_id_cidade) VALUES (2, 'Pinheiros', 1);
INSERT INTO bairro (id_bairro, nm_bairro, cidade_id_cidade) VALUES (3, 'Sé', 1);
INSERT INTO bairro (id_bairro, nm_bairro, cidade_id_cidade) VALUES (4, 'Mooca', 1);
INSERT INTO bairro (id_bairro, nm_bairro, cidade_id_cidade) VALUES (5, 'Santana', 1);

-- 4. ENDERECO
INSERT INTO endereco (id_endereco, cep, rua, numero, referencia, bairro_id_bairro) 
VALUES (1, '01311000', 'Avenida Paulista', '1106', 'Próximo ao MASP', 1);

INSERT INTO endereco (id_endereco, cep, rua, numero, referencia, bairro_id_bairro) 
VALUES (2, '05422000', 'Rua dos Pinheiros', '450', 'Esquina com Rua Fradique Coutinho', 2);

INSERT INTO endereco (id_endereco, cep, rua, numero, referencia, bairro_id_bairro) 
VALUES (3, '01001000', 'Praça da Sé', '100', 'Em frente à Catedral da Sé', 3);

INSERT INTO endereco (id_endereco, cep, rua, numero, referencia, bairro_id_bairro) 
VALUES (4, '03104001', 'Rua da Mooca', '1500', 'Ao lado da estação Mooca', 4);

INSERT INTO endereco (id_endereco, cep, rua, numero, referencia, bairro_id_bairro) 
VALUES (5, '02030000', 'Avenida Cruzeiro do Sul', '2500', 'Em frente ao Metrô Santana', 5);

-- 5. USUARIO (Senha compatível com Spring Security BCrypt: 'Senha#123' ou 'Senha@123')
INSERT INTO usuario (id_usuario, nm_usuario, email, senha, cargo, endereco_id_endereco) 
VALUES (1, 'Carlos Alberto Silva', 'carlos.silva@email.com', '$2a$10$nFdyYsVy8uiDRBO5KBpBWu9RKUQsIAM36oG9sK3ABRpcGike3MLv.', 'CIDADAO', 1);

INSERT INTO usuario (id_usuario, nm_usuario, email, senha, cargo, endereco_id_endereco) 
VALUES (2, 'Mariana Oliveira Souza', 'mariana.souza@email.com', '$2a$10$nFdyYsVy8uiDRBO5KBpBWu9RKUQsIAM36oG9sK3ABRpcGike3MLv.', 'CIDADAO', 2);

INSERT INTO usuario (id_usuario, nm_usuario, email, senha, cargo, endereco_id_endereco) 
VALUES (3, 'Eng. Roberto Santos', 'roberto.gestor@subprefeitura.sp.gov.br', '$2a$10$nFdyYsVy8uiDRBO5KBpBWu9RKUQsIAM36oG9sK3ABRpcGike3MLv.', 'GESTOR', 3);

INSERT INTO usuario (id_usuario, nm_usuario, email, senha, cargo, endereco_id_endereco) 
VALUES (4, 'Fernanda Costa Lima', 'fernanda.costa@email.com', '$2a$10$nFdyYsVy8uiDRBO5KBpBWu9RKUQsIAM36oG9sK3ABRpcGike3MLv.', 'CIDADAO', 4);

INSERT INTO usuario (id_usuario, nm_usuario, email, senha, cargo, endereco_id_endereco) 
VALUES (5, 'Coordenador Lucas Mendes', 'lucas.zeladoria@subprefeitura.sp.gov.br', '$2a$10$nFdyYsVy8uiDRBO5KBpBWu9RKUQsIAM36oG9sK3ABRpcGike3MLv.', 'GESTOR', 5);

-- 6. TELEFONE
INSERT INTO telefone (id_telefone, num_ddd, num_tel, usuario_id_usuario) VALUES (1, '11', '981234567', 1);
INSERT INTO telefone (id_telefone, num_ddd, num_tel, usuario_id_usuario) VALUES (2, '11', '972345678', 2);
INSERT INTO telefone (id_telefone, num_ddd, num_tel, usuario_id_usuario) VALUES (3, '11', '963456789', 3);
INSERT INTO telefone (id_telefone, num_ddd, num_tel, usuario_id_usuario) VALUES (4, '11', '954567890', 4);
INSERT INTO telefone (id_telefone, num_ddd, num_tel, usuario_id_usuario) VALUES (5, '11', '945678901', 5);

-- 7. CATEGORIA
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (1, 'Iluminação Pública');
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (2, 'Vias e Pavimentação');
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (3, 'Drenagem e Esgoto');
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (4, 'Limpeza Urbana');
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (5, 'Árvores e Parques');
INSERT INTO categoria (id_categoria, nm_categoria) VALUES (6, 'Sinalização e Trânsito');

-- 8. STATUS
INSERT INTO status (id_status, nm_status) VALUES (1, 'ABERTO');
INSERT INTO status (id_status, nm_status) VALUES (2, 'EM_ANALISE');
INSERT INTO status (id_status, nm_status) VALUES (3, 'EM_EXECUCAO');
INSERT INTO status (id_status, nm_status) VALUES (4, 'CONCLUIDO');
INSERT INTO status (id_status, nm_status) VALUES (5, 'REJEITADO');

-- 9. CHAMADO (com foto BLOB)
INSERT INTO chamado (
    id_chamado, titulo, descricao, foto, dt_abertura,
    usuario_id_usuario, endereco_id_endereco, categoria_id_categoria
) VALUES (
    1,
    'Poste com lâmpada apagada em via principal',
    'Poste de iluminação pública em frente ao número 1106 apagado há 4 noites consecutivas, gerando sensação de insegurança aos pedestres.',
    UTL_RAW.CAST_TO_RAW('FOTO_BINARIA_DEMO_POSTE_1'),
    DATE '2026-11-01',
    1, 1, 1
);

INSERT INTO chamado (
    id_chamado, titulo, descricao, foto, dt_abertura,
    usuario_id_usuario, endereco_id_endereco, categoria_id_categoria
) VALUES (
    2,
    'Buraco profundo na pista da direita',
    'Cratera aberta no asfalto próxima à faixa de ônibus, oferecendo alto risco de acidentes para motociclistas e danos aos veículos.',
    UTL_RAW.CAST_TO_RAW('FOTO_BINARIA_DEMO_BURACO_2'),
    DATE '2026-11-02',
    2, 2, 2
);

-- 10. HIST_CHAMADO
INSERT INTO hist_chamado (id_hist, dt_mudanca, observacoes, chamado_id_chamado, usuario_id_usuario, status_id_status)
VALUES (1, DATE '2026-11-01', 'Chamado registrado pelo cidadão Carlos Alberto via ZelaFix Mobile.', 1, 1, 1);

INSERT INTO hist_chamado (id_hist, dt_mudanca, observacoes, chamado_id_chamado, usuario_id_usuario, status_id_status)
VALUES (2, DATE '2026-11-02', 'Triagem concluída: demanda encaminhada ao Departamento de Iluminação Pública.', 1, 3, 2);

INSERT INTO hist_chamado (id_hist, dt_mudanca, observacoes, chamado_id_chamado, usuario_id_usuario, status_id_status)
VALUES (3, DATE '2026-11-02', 'Chamado registrado pela cidadã Mariana Oliveira via ZelaFix Mobile.', 2, 2, 1);

-- 11. VOTO_CHAMADO
INSERT INTO voto_chamado (id_voto, dt_voto, chamado_id_chamado, usuario_id_usuario) VALUES (1, DATE '2026-11-01', 1, 1);
INSERT INTO voto_chamado (id_voto, dt_voto, chamado_id_chamado, usuario_id_usuario) VALUES (2, DATE '2026-11-02', 1, 2);
INSERT INTO voto_chamado (id_voto, dt_voto, chamado_id_chamado, usuario_id_usuario) VALUES (3, DATE '2026-11-02', 2, 2);
