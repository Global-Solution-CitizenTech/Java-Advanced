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
INSERT INTO categoria (nm_categoria) VALUES ('Iluminação Pública');
INSERT INTO categoria (nm_categoria) VALUES ('Vias e Pavimentação');
INSERT INTO categoria (nm_categoria) VALUES ('Drenagem e Esgoto');
INSERT INTO categoria (nm_categoria) VALUES ('Limpeza Urbana');
INSERT INTO categoria (nm_categoria) VALUES ('Árvores e Parques');
INSERT INTO categoria (nm_categoria) VALUES ('Sinalização e Trânsito');

-- 5. STATUS
INSERT INTO status (nm_status) VALUES ('PENDENTE');
INSERT INTO status (nm_status) VALUES ('ABERTO');
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
