-- =========================================================================
-- V3: Atualização de Constraints, Status PENDENTE e Criação do ADMIN Inicial (Oracle 19c/21c/23c)
-- Ecossistema ZelaFix (CitizenTech)
-- =========================================================================

-- 1. Atualização de constraints para compatibilidade com bases existentes
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(1) INTO v_count FROM user_constraints WHERE constraint_name = 'CK_USUARIO_CARGO';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE usuario DROP CONSTRAINT ck_usuario_cargo';
    END IF;
    EXECUTE IMMEDIATE 'ALTER TABLE usuario ADD CONSTRAINT ck_usuario_cargo CHECK (cargo IN (''CIDADAO'', ''GESTOR'', ''ADMIN''))';

    SELECT COUNT(1) INTO v_count FROM user_constraints WHERE constraint_name = 'CK_STATUS_NOME';
    IF v_count > 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE status DROP CONSTRAINT ck_status_nome';
    END IF;
    EXECUTE IMMEDIATE 'ALTER TABLE status ADD CONSTRAINT ck_status_nome CHECK (nm_status IN (''PENDENTE'', ''EM_ANALISE'', ''EM_EXECUCAO'', ''CONCLUIDO'', ''REJEITADO'', ''ABERTO''))';
END;
/

-- 2. Garantia do status PENDENTE (usando IDENTITY nativo, sem MAX+1)
DECLARE
    v_existe_pendente NUMBER := 0;
BEGIN
    SELECT COUNT(1) INTO v_existe_pendente FROM status WHERE nm_status = 'PENDENTE';
    IF v_existe_pendente = 0 THEN
        INSERT INTO status (nm_status) VALUES ('PENDENTE');
    END IF;
END;
/

-- 3. Criação ou atualização do Administrador padrão (senha '123mudar') via IDENTITY + RETURNING
DECLARE
    v_hash VARCHAR2(100) := TRIM('${ADMIN_BCRYPT_HASH}');
    v_existe_admin NUMBER := 0;
    v_user_id NUMBER(5);
    v_end_id NUMBER(5);
    v_bairro_id NUMBER(5);
BEGIN
    IF v_hash IS NULL OR LENGTH(v_hash) < 50 THEN
        v_hash := '$2a$10$SXCHKmagXlo4hjWTaTfWIeGT6Qh8BWLEUgWHsxPEWDxKpQX1Q96Yi';
    END IF;

    SELECT COUNT(1) INTO v_existe_admin FROM usuario WHERE email = 'admin@zelafix.com.br';

    IF v_existe_admin = 0 THEN
        BEGIN
            SELECT id_endereco INTO v_end_id FROM endereco WHERE ROWNUM = 1;
        EXCEPTION WHEN NO_DATA_FOUND THEN
            SELECT id_bairro INTO v_bairro_id FROM bairro WHERE ROWNUM = 1;
            INSERT INTO endereco (cep, rua, numero, referencia, bairro_id_bairro)
            VALUES ('01311000', 'Avenida Paulista', '1000', 'Gabinete de Tecnologia', v_bairro_id)
            RETURNING id_endereco INTO v_end_id;
        END;

        INSERT INTO usuario (nm_usuario, email, senha, cargo, endereco_id_endereco)
        VALUES ('Administrador Geral', 'admin@zelafix.com.br', v_hash, 'ADMIN', v_end_id)
        RETURNING id_usuario INTO v_user_id;

        INSERT INTO telefone (num_ddd, num_tel, usuario_id_usuario)
        VALUES ('11', '999990000', v_user_id);
    ELSE
        UPDATE usuario 
        SET senha = v_hash, cargo = 'ADMIN', nm_usuario = 'Administrador Geral'
        WHERE email = 'admin@zelafix.com.br';
    END IF;
END;
/

-- 4. Alinhamento preventivo das sequences IDENTITY após carga inicial
BEGIN
    FOR r IN (SELECT table_name, column_name, generation_type FROM user_tab_identity_cols) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE "' || r.table_name || '" MODIFY ("' || r.column_name || '" GENERATED ' || r.generation_type || ' AS IDENTITY (START WITH LIMIT VALUE))';
    END LOOP;
END;
/
