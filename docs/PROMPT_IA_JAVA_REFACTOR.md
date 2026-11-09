# PROMPT DE REFATORAÇÃO DO BACKEND JAVA ADVANCED (Spring Boot + RabbitMQ + Spring AI)

> **Instruções:** Copie todo o conteúdo abaixo e envie diretamente para o assistente de IA responsável pelo módulo **Java-Advanced**.

---

```markdown
# TAREFA CRÍTICA: REFATORAÇÃO DO BACKEND JAVA (ZelaFix) PARA ALINHAMENTO COM O BANCO ORACLE 3FN

Você é o engenheiro especialista em Java e Spring Boot responsável pelo ecossistema **ZelaFix** (FIAP Global Solution 2026/2 - Turma 2TDSPG).

## 1. CONTEXTO & REGRA DE OURO
A modelagem do banco de dados relacional foi 100% fechada e normalizada no **Oracle SQL Developer Data Modeler** seguindo estrita **3ª Forma Normal (3FN)** (arquivo canonical: `Database-Advanced/data modeler/citizenTech.ddl` e script `Database-Advanced/2TDSPG_2026_CodigoSql_ZelaFix.sql`).

⚠️ **REGRA DE OURO:** O backend Java (`zelafix-api` e `zelafix-worker`) **DEVE SE CURVAR AO BANCO DE DADOS**, adaptando suas entidades, DTOs, controllers e repositórios para bater **100% campo a campo** com as 11 tabelas do Oracle. Nenhuma coluna inexistente no banco pode ser referenciada pelo Hibernate/JPA, sob pena de crash imediato na inicialização (`ORA-00904` ou `SchemaManagementException`).

---

## 2. AS 11 TABELAS OFICIAIS DO ORACLE (citizenTech.ddl)
O banco Oracle possui **EXATAMENTE** e **APENAS** as tabelas e colunas abaixo:

1. **`estado`**: `id_estado NUMBER(5) PK`, `nm_estado VARCHAR2(30)`
2. **`cidade`**: `id_cidade NUMBER(5) PK`, `nm_cidade VARCHAR2(50)`, `estado_id_estado NUMBER(5) FK`
3. **`bairro`**: `id_bairro NUMBER(5) PK`, `nm_bairro VARCHAR2(50)`, `cidade_id_cidade NUMBER(5) FK`
4. **`endereco`**: `id_endereco NUMBER(5) PK`, `cep VARCHAR2(8)`, `rua VARCHAR2(100)`, `numero VARCHAR2(100)`, `referencia VARCHAR2(50)`, `bairro_id_bairro NUMBER(5) FK`  
   *(Atenção: NÃO existe coluna `complemento`! A coluna oficial é `referencia`)*
5. **`usuario`**: `id_usuario NUMBER(5) PK`, `nm_usuario VARCHAR2(100)`, `email VARCHAR2(100) UNIQUE`, `senha VARCHAR2(60)`, `cargo VARCHAR2(10) CHECK ('CIDADAO', 'GESTOR')`, `endereco_id_endereco NUMBER(5) FK`  
   *(Atenção: NÃO existem as colunas `cpf`, `avatar_url` nem `dt_cadastro`!)*
6. **`telefone`**: `id_telefone NUMBER(5) PK`, `num_ddd VARCHAR2(2)`, `num_tel VARCHAR2(9)`, `usuario_id_usuario NUMBER(5) FK`
7. **`categoria`**: `id_categoria NUMBER(5) PK`, `nm_categoria VARCHAR2(50)`
8. **`status`**: `id_status NUMBER(5) PK`, `nm_status VARCHAR2(20)` (`ABERTO`, `EM_ANALISE`, `EM_EXECUCAO`, `CONCLUIDO`, `REJEITADO`)
9. **`chamado`**:  
   - `id_chamado NUMBER(5) PK`
   - `titulo VARCHAR2(100) NOT NULL`
   - `descricao VARCHAR2(1000) NOT NULL`
   - `foto BLOB NOT NULL` *(A imagem é armazenada em formato binário BLOB nativo, NÃO existe `foto_url` no banco!)*
   - `dt_abertura DATE NOT NULL`
   - `usuario_id_usuario NUMBER(5) FK NOT NULL`
   - `endereco_id_endereco NUMBER(5) FK NOT NULL`
   - `categoria_id_categoria NUMBER(5) FK NOT NULL`  
   *(Atenção Máxima: No `chamado` NÃO existem: `status_id_status`, `dt_conclusao`, `protocolo`, `prioridade`, `parecer_ia`, `orgao_responsavel`, `feedback_resolucao`, `apoios_count`)*
10. **`hist_chamado`** *(nome no SINGULAR, não é `hist_status`)*:  
    - `id_hist NUMBER(5) PK`
    - `dt_mudanca DATE NOT NULL`
    - `observacoes VARCHAR2(255) NOT NULL`
    - `chamado_id_chamado NUMBER(5) FK NOT NULL`
    - `usuario_id_usuario NUMBER(5) FK NOT NULL`
    - `status_id_status NUMBER(5) FK NOT NULL`
11. **`voto_chamado`**:  
    - `id_voto NUMBER(5) PK`
    - `dt_voto DATE NOT NULL`
    - `chamado_id_chamado NUMBER(5) FK NOT NULL`
    - `usuario_id_usuario NUMBER(5) FK NOT NULL`  
    - Constraint: `voto_chamado_un UNIQUE (chamado_id_chamado, usuario_id_usuario)`

---

## 3. DIRETRIZES DE REFATORAÇÃO NO JAVA ADVANCED

### A. Configuração do Hibernate & Flyway (`zelafix-api/src/main/resources/application.properties`)
1. Alterar:
   ```properties
   spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
   spring.flyway.enabled=${FLYWAY_ENABLED:false}
   ```
   *Justificativa:* Como a base de dados Oracle já é criada e populada via scripts oficiais da disciplina de banco (`Database-Advanced`), o Flyway não deve rodar migrations duplicadas para evitar `ORA-00955: name is already used by an existing object`.

---

### B. Entidade `Chamado.java` (`fiap.com.br.zelafix.chamado.Chamado`)
1. Remover colunas físicas inexistentes do `@Table(name = "chamado")`:
   - Remover `@Column(name = "foto_url")`
   - Remover `@Column(name = "protocolo")`
   - Remover `@Column(name = "prioridade")`
   - Remover `@Column(name = "parecer_ia")`
   - Remover `@Column(name = "orgao_responsavel")`
   - Remover `@Column(name = "feedback_resolucao")`
   - Remover `@Column(name = "apoios_count")`
   - Remover `@Column(name = "dt_conclusao")`
   - Remover o mapeamento de coluna `@JoinColumn(name = "status_id_status")`
2. Mapear a foto como array binário BLOB:
   ```java
   @Lob
   @Column(name = "foto", nullable = false)
   private byte[] foto;
   ```
3. Mapear o Histórico de Status e os Votos como relacionamentos:
   ```java
   @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
   @OrderBy("idHist DESC")
   @Builder.Default
   private List<HistChamado> historico = new ArrayList<>();

   @OneToMany(mappedBy = "chamado", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
   @Builder.Default
   private List<VotoChamado> votos = new ArrayList<>();
   ```
4. Criar métodos getters auxiliares/transientes na entidade `Chamado` para que a API continue fornecendo as informações aos DTOs sem quebrar o front/mobile:
   ```java
   public Status getStatus() {
       return (historico != null && !historico.isEmpty()) ? historico.get(0).getStatus() : null;
   }

   public int getApoiosCount() {
       return (votos != null) ? votos.size() : 0;
   }

   public String getProtocolo() {
       return String.format("ZFX-%05d", idChamado != null ? idChamado : 0);
   }

   public LocalDateTime getDtConclusao() {
       if (historico != null) {
           for (HistChamado h : historico) {
               if ("CONCLUIDO".equalsIgnoreCase(h.getStatus().getNmStatus())) {
                   return h.getDtMudanca();
               }
           }
       }
       return null;
   }
   ```

---

### C. Entidade `HistChamado.java` (Renomear de `HistStatus.java`)
1. Pacote `fiap.com.br.zelafix.historico`:
   - Renomear a classe de `HistStatus` para `HistChamado`.
   - Ajustar a anotação: `@Table(name = "hist_chamado")`.
   - Mapear atributos:
     - `Long idHist` (`id_hist`)
     - `LocalDateTime dtMudanca` (`dt_mudanca`)
     - `String observacoes` (`observacoes`, length 255)
     - `@ManyToOne Chamado chamado` (`chamado_id_chamado`)
     - `@ManyToOne Usuario usuario` (`usuario_id_usuario`)
     - `@ManyToOne Status status` (`status_id_status`)

---

### D. Entidade `Endereco.java`
1. Remover o campo `complemento`.
2. Adicionar o campo obrigatório:
   ```java
   @Column(name = "referencia", nullable = false, length = 50)
   private String referencia;
   ```
3. Ajustar `EnderecoResponse.java` e `EnderecoRequest.java` para refletir `referencia`.

---

### E. Entidade `Usuario.java`
1. Remover os campos que não existem no banco Oracle:
   - Remover `cpf`
   - Remover `avatarUrl`
   - Remover `dtCadastro`
2. Manter apenas: `idUsuario`, `nmUsuario`, `email`, `senha`, `cargo`, `endereco` e a lista de `telefones`.
3. Ajustar `UsuarioRequest` e `UsuarioResponse` para não exigir/retornar esses campos inexistentes.

---

### F. Endpoint de Imagem BLOB em `ChamadoController.java`
Para que o app Mobile e o Swagger consumam a foto armazenada no banco Oracle sem depender de URLs externas:
1. Adicionar endpoint REST em `ChamadoController.java`:
   ```java
   @GetMapping(value = "/{id}/foto", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
   @Operation(summary = "Obter a imagem fotográfica da ocorrência armazenada em BLOB no Oracle")
   public ResponseEntity<byte[]> buscarFotoChamado(@PathVariable Long id) {
       Chamado chamado = chamadoService.buscarEntidadePorId(id);
       if (chamado.getFoto() == null || chamado.getFoto().length == 0) {
           return ResponseEntity.notFound().build();
       }
       return ResponseEntity.ok()
               .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"chamado-" + id + ".jpg\"")
               .body(chamado.getFoto());
   }
   ```
2. No `ChamadoResponse.java`, o campo `fotoUrl` deve retornar a rota relativa do endpoint: `/chamados/{id}/foto`.
3. No `ChamadoCreateRequest.java`, permitir receber a foto em `MultipartFile` ou Base64 decodificada para `byte[]`.

---

### G. Repositório `ChamadoRepository.java`
Ajustar a consulta dinâmica JPQL `findByFiltros` para filtrar por status consultando o último histórico:
```java
@Query("""
    SELECT c FROM Chamado c
    WHERE (:status IS NULL OR EXISTS (
        SELECT h FROM HistChamado h 
        WHERE h.chamado = c 
          AND LOWER(h.status.nmStatus) = LOWER(:status)
          AND h.idHist = (SELECT MAX(h2.idHist) FROM HistChamado h2 WHERE h2.chamado = c)
    ))
      AND (:categoria IS NULL OR LOWER(c.categoria.nmCategoria) = LOWER(:categoria))
      AND (:usuarioId IS NULL OR c.usuario.idUsuario = :usuarioId)
      AND (:busca IS NULL OR LOWER(c.titulo) LIKE LOWER(CONCAT('%', :busca, '%'))
                         OR LOWER(c.descricao) LIKE LOWER(CONCAT('%', :busca, '%'))
                         OR LOWER(c.endereco.bairro.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
""")
Page<Chamado> findByFiltros(
        @Param("status") String status,
        @Param("categoria") String categoria,
        @Param("usuarioId") Long usuarioId,
        @Param("busca") String busca,
        Pageable pageable
);
```

---

### H. Worker RabbitMQ (`zelafix-worker`)
1. No worker, a entidade `ChamadoWorkerEntity.java` não deve tentar persistir colunas inexistentes na tabela `chamado`.
2. O parecer gerado pela **Spring AI** (`ChamadoAiConsumer.java`):
   - Deve registrar o parecer da IA no log e gravá-lo no `hist_chamado` (campo `observacoes`) como `[Triagem Spring AI - Órgão: ... | Urgência: ...]`, OU persistir no banco NoSQL **MongoDB** (que é o local documental para agregação analítica da GS2).
3. O `VotoConsumer.java`:
   - Não deve atualizar a coluna inexistente `apoios_count` em `chamado`. Deve salvar os votos na tabela associativa `voto_chamado` ou sincronizar o cache em lote no MongoDB.

---

## 4. CRITÉRIOS DE ACEITE & COMPILAÇÃO
Ao concluir as modificações:
1. Executar `./gradlew compileJava` na raiz do projeto e certificar-se de que compila com **ZERO erros**.
2. Executar os testes automatizados com `./gradlew test`.
3. Garantir que todas as tabelas e colunas citadas batem 100% com o `citizenTech.ddl`.
```
