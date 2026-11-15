# 🤖 Guia Operacional do Agente — ZelaFix Backend (Java Advanced)
> **Padrão:** Clean Architecture & Ponytail Standard  
> **Projeto:** FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)  
> **Disciplina:** Java Advanced  
> **Opção de Distribuição:** Opção A — Mensageria Assíncrona com RabbitMQ (Padrão de Arquitetura Distribuída Reativa)  
> **Esteira CI/CD:** Azure DevOps (`ZelaFix-CI-CD`) & GitHub Actions (`mirror-to-azure.yml`)

---

## 🏛️ 1. Arquitetura do Sistema e Divisão de Responsabilidades

O backend do ecossistema **ZelaFix** é estruturado em **dois microsserviços autônomos e desacoplados** que operam de forma cooperativa através do broker de mensageria **RabbitMQ**:

```
                              ┌──────────────────────────────────────────────┐
                              │            Client Layer (Mobile/Web)         │
                              └──────────────────────┬───────────────────────┘
                                                     │ HTTP REST / JWT Bearer (RSA)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-api (Porta 8080) — Gateway RESTful de Domínio & Persistência ACID                             │
│  - Gateway RESTful consumido pelo app React Native Expo e painéis administrativos                      │
│  - Spring Security com JWT RSA (Nimbus) & RBAC (ROLE_CIDADAO, ROLE_GESTOR, ROLE_ADMIN)                 │
│  - Persistência exclusiva no Banco Relacional Oracle FIAP via Spring Data JPA / Hibernate 7.4          │
│  - Java 21 Project Loom: Virtual Threads ativas (spring.threads.virtual.enabled=true) para I/O leve    │
│  - Versionamento automático de banco com Flyway (V1 DDL 13 tabelas, V2 Domínios + Admin, V3 Seed)      │
│  - Validação estrita na borda (Jakarta Bean Validation nos Request DTOs + @DddValidation)              │
│  - Tratamento global de erros padronizado RFC 7807 (GlobalExceptionHandler)                            │
│  - Caching declarativo de catálogo com @Cacheable/@CacheEvict (Categoria, Status, Prioridade, Órgão)   │
│  - Cliente HTTP declarativo com @HttpExchange / @GetExchange e @ImportHttpServices (ViaCepService)     │
│  - Produtor RabbitMQ + DeferredResult: publica endereço + foto no zelafix.direct e aguarda triagem     │
│  - Consumidor RabbitMQ (Feedback): recusa imagens sem persistir ou grava chamado com histórico duplo   │
│    conectado via saveAll (PENDENTE inicial de abertura pelo munícipe + EM_ANALISE triado pela IA)      │
│  - Consumidor RabbitMQ (Prioridade): escala prioridade no Oracle sob mobilização popular comunitária │
└───────────────────────┬────────────────────────────────────────────────────▲───────────────────────────┘
                        │                                                    │
                        │ chamado.triagem.key                                │ chamado.triagem-concluida.key
                        │ chamado.voto.key                                   │ chamado.prioridade-escalada.key
                        ▼                                                    │
┌────────────────────────────────────────────────────────────────────────────┴───────────────────────────┐
│  RabbitMQ Broker (Porta 5672) — Exchange: zelafix.direct (DirectExchange)                              │
│  - Fila: chamado.triagem.queue (Durable)                                                               │
│  - Fila: chamado.triagem-concluida.queue (Durable)                                                     │
│  - Fila: chamado.voto.queue (Durable)                                                                  │
│  - Fila: chamado.prioridade-escalada.queue (Durable)                                                   │
└───────────────────────┬────────────────────────────────────────────────────────────────────────────────┘
                        │
                        │ Consumo Assíncrono
                        ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-worker (Porta 8081) — Stateless Heavy Worker                                                  │
│  - Consumidor assíncrono de alta vazão (RabbitMQ @RabbitListener com Virtual Threads Loom)             │
│  - Inteligência Artificial Generativa e Multimodal com Spring AI (LLM Multimodal + Tool Calling)       │
│  - Arquitetura Fail-Fast imediata com fallback gracioso PENDENTE via ChamadoAiConsumer                 │
│  - Detecção dinâmica de MIME type via magic bytes e redimensionamento JPEG (ImageMimeTypeDetector)     │
│  - Agregador de votos em buffer volátil com detecção de limiares (>=3 ALTA, >=5 URGENTE)               │
│  - ZERO CONEXÃO COM BANCO DE DADOS: 100% Stateless, sem JDBC, sem JPA, sem Oracle, sem locks           │
│  - Produtor de feedback e escalonamento comunitário para chamado.prioridade-escalada.queue             │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📂 2. Arquitetura de Diretórios: Gradle Multi-Project

### Justificativa Técnica Corporativa
> *"Adotamos a arquitetura de **Gradle Multi-Project**, que é a boa prática corporativa para microsserviços que compartilham o mesmo ciclo de release. Ela nos permite ter controle centralizado das versões do Spring Boot (4.1.1) e Java (21 LTS), compilar e testar toda a esteira de CI/CD de forma atômica no Azure DevOps em um único comando (`call gradlew.bat clean test bootJar`), e ao mesmo tempo manter `zelafix-api` e `zelafix-worker` como artefatos completamente desacoplados e independentes em tempo de execução."*

### Estrutura de Arquivos:
- `build.gradle` (raiz): Centraliza plugins (`org.springframework.boot` apply false, `io.spring.dependency-management` apply false).
- `settings.gradle`: Declara `rootProject.name = 'zelafix-backend'` e inclui `include 'zelafix-api', 'zelafix-worker'`.
- `zelafix-api/`: Dependências de banco Oracle, JPA, Flyway, Spring Security e AMQP.
- `zelafix-worker/`: Dependências de Spring Boot WebMVC, Actuator, Spring AMQP e Spring AI OpenAI. **Proibido adicionar dependências de banco aqui.**

---

## 🚫 3. Ausência Total de Banco de Dados no `zelafix-worker`

Uma das decisões arquiteturais mais críticas do projeto é a **completa ausência de banco de dados relacional ou transacional no microsserviço `zelafix-worker`**:

1. **Desacoplamento e Isolamento de Falhas:**
   - O worker atua como um *pure compute / inference worker*. Se o banco de dados sofrer lentidão momentânea, o processamento de inferência da IA continua fluindo e enfileirado no RabbitMQ sem provocar saturação de pool de conexões (HikariCP) ou deadlocks.
2. **Escalabilidade Horizontal Instantânea:**
   - Como o worker é estritamente *stateless*, novas instâncias do `zelafix-worker` inicializam em ~2 segundos em contêineres sem disputar licenças de conexões do Oracle corporativo.
3. **Fluxo Bidirecional via Mensageria (Feedback Pattern):**
   - O worker **não escreve diretamente nas tabelas**. Quando conclui a inferência técnica via LLM, o worker empacota o resultado em um payload JSON (`TriagemFeedbackMessage`) e o publica de volta na fila `chamado.triagem-concluida.queue`.
   - A `zelafix-api`, detentora legal e proprietária exclusiva do schema relacional, consome o feedback via `ChamadoTriagemFeedbackConsumer` e realiza a transação ACID no Oracle com auditoria histórica (`HistChamado`).
4. **Agregação Volátil de Votos e Escalonamento Comunitário:**
   - O `VotoConsumer` armazena deltas de apoios em memória utilizando `ConcurrentHashMap<Long, Integer>`. O flush periódico a cada 3 segundos (`@Scheduled`) avalia se o chamado atingiu limiares comunitários ($\ge 3$ apoios $\rightarrow$ `ALTA`, $\ge 5$ apoios $\rightarrow$ `URGENTE`). Ao atingir, publica `PrioridadeEscaladaMessage` em `chamado.prioridade-escalada.queue`. A API consome o evento e eleva a prioridade no Oracle com histórico em `HistChamado`.

---

## 🔄 4. Topologia e Fluxo de Mensageria RabbitMQ (`zelafix.direct`)

Toda a comunicação assíncrona entre os dois serviços é orquestrada por uma única `DirectExchange` nomeada `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Tipo de Payload | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Dispara a triagem técnica multimodal com Spring AI levando a foto da ocorrência em bytes. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Devolve o parecer da IA (prioridade, órgão competente e parecer técnico) para que a API atualize o banco. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica apoios/votos em ocorrências para agregação em lote em alta vazão. |
| `chamado.prioridade-escalada.queue` | `chamado.prioridade-escalada.key` | `zelafix-worker` | `zelafix-api` | `PrioridadeEscaladaMessage` | Notifica a API para elevar a prioridade do chamado no Oracle quando o volume de apoios atinge os limiares comunitários. |

### Serialização e Ausência de `Serializable`
- Todas as mensagens utilizam **Jackson JSON Converter** (`JacksonJsonMessageConverter("*")`) com `converter.setAlwaysConvertToInferredType(true)`.
- É **estritamente proibido** o uso de `java.io.Serializable` nos DTOs de mensageria. As mensagens transitam como JSON canônico (UTF-8).

---

## 🧠 5. Spring AI e Triagem Inteligente Multimodal Obrigatória (`ZelaFixAiService`)

No ZelaFix, todo chamado registrado na plataforma requer foto da ocorrência (`BLOB NOT NULL` no banco de dados):

- **Injeção Idiomática do `ChatClient`:**
  - Injeção direta de `ChatClient.Builder` no construtor com `SYSTEM_PROMPT` normativo, `defaultTools(triagemTools)` e `SimpleLoggerAdvisor`.
  - Configuração desacoplada de modelo via `AI_BASE_URL`, `AI_API_KEY` e `AI_MODEL` (padrão OpenAI API compatível, ex.: OpenRouter `particle-ai/particle-1:free`).
- **Validação de Escopo e Diretrizes Normativas:**
  - `ocorrenciaValida`: `true` apenas se for foto real de espaço público urbano com problema visível de manutenção; `false` se for print de celular, meme, selfie, objeto ou ambiente interno.
  - Quando recusado, o chamado não é persistido no Oracle e a API responde HTTP `400 Bad Request` com o motivo gerado pela IA.
- **Tool Calling Declarativo (`TriagemNormativaTools`):**
  - `@Tool consultarNormaMunicipal`: Consulta decretos municipais PMSP e órgão padrão da categoria.
  - `@Tool calcularPrazoSla`: Calcula prazo em horas e data limite conforme a prioridade.
- **Detecção Automática e Otimização de MimeType (`ImageMimeTypeDetector`):**
  - Detecção por magic bytes (`image/jpeg`, `image/png`, `image/webp`, `image/gif`).
  - Redimensionamento automático (`redimensionarParaIa` com max dimension de 768px em JPEG qualidade 0.85) para reduzir tokens e acelerar a inferência.
- **Arquitetura Fail-Fast & Contingência Graciosa:**
  - Sem `@Retryable` no LLM para evitar multiplicação de tempo e manter tempo de resposta rápido (3s a 5s).
  - Em caso de falha de conexão ou esgotamento de quota, o `ChamadoAiConsumer` captura a exceção imediatamente e emite `TriagemFeedbackMessage.contingencia(message)` com status `PENDENTE` e encaminhamento para análise humana.
- **Structured Output Tipado e Proteção UTF-8 Oracle:**
  - Retorno mapeado e validado via `ChatClient.EntityParamSpec::validateSchema` com truncamento UTF-8 por code-points (`comLimitesOracle()`):
    ```java
    public record TriagemIaResult(
        boolean ocorrenciaValida,
        String titulo,                        // Máx. 70 caracteres (VARCHAR2 100 BYTE)
        String descricao,                     // Máx. 500 caracteres (VARCHAR2 1000 BYTE)
        CategoriaSugerida categoriaSugerida,  // Enums oficiais da plataforma
        PrioridadeChamado prioridade,         // BAIXA, MEDIA, ALTA, URGENTE
        DepartamentoGoverno orgaoResponsavel  // SECRETARIA_OBRAS, CET_TRANSITO, etc.
    ) {}
    ```

---

## 🗄️ 6. Flyway Database Migrations e Integridade Relacional

O versionamento do schema corporativo Oracle segue o padrão incremental estrito:
- **`V1__criar_tabelas_zelafix.sql`:** DDL canônico conforme especificação da FIAP. Sequências `IDENTITY` declaradas com `START WITH 100` para permitir cargas e evitar colisões.
- **`V2__dados_iniciais_zelafix.sql`:** Carga controlada de dados mestres (Estados, Cidades, Bairros de São Paulo, Categorias e Status).
- **`V3__admin_inicial.sql`:** Usuários padrão do sistema com senhas criptografadas em BCrypt (`ROLE_CIDADAO`, `ROLE_GESTOR`, `ROLE_ADMIN`).
- **Regra do Agente:** Nunca modifique o DDL canônico de migrations já aplicadas. Não utilize blocos PL/SQL anônimos desnecessários; use a convenção padrão do Flyway.

---

## 🧹 7. Convenções de Código Limpo e Ponytail Standard

Para manter o código sustentável, seguro e harmonioso, todos os módulos obedecem às seguintes regras:

### 7.1 Fronteira de Confiança (Validação na Borda)
- **Bean Validation no Controller:** Todo payload HTTP de entrada é validado na borda via `@Valid` e anotações nos Request DTOs (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@DddValidation`, etc.).
- **Proibição de Código Defensivo Redundante:** Não polua Services, mappers ou métodos de domínio com checagens nulas de campos que já foram validados na borda.
- **Modelagem Fiel ao DDL:** Não adicionar campos fantasmas que não existem nas tabelas do banco (ex: `dt_nascimento` não existe na tabela `usuario`).

### 7.2 Java Records, DTOs Puros e Métodos de Conversão
- **Java Records:** Todos os DTOs (Request, Response e Queue Messages) são declarados como `public record`.
- **Sem Sobrecargas Desnecessárias:**
  - Os DTOs de Response devem conter unicamente o factory method canônico:
    - `public static XResponse fromEntity(Entity e)`
    - `ChamadoResponse.fromEntity(Chamado c, String status, long totalVotos, boolean apoiado, List<HistChamado> historicos)` (recebendo a lista de históricos e montando internamente a resposta).
    - `HistoricoResponse.fromEntity(HistChamado h)`
    - `EstatisticasResponse.of(...)`
- **Request DTOs:** Não devem possuir métodos mutadores de entidades (ex: `applyTo`). A mutação de entidades para edição pertence a métodos privados `aplicarEm` nos respectivos Services.
- **Conversão DTO -> Entidade:** Feita exclusivamente através de métodos `toEntity(...)` dentro do DTO de Request ou mensagem de fila (ex: `ChamadoRequest.toEntity(...)`, `ChamadoStatusRequest.toEntity(...)`, `VotoMessage.toEntity(...)`, `TriagemFeedbackMessage.toEntity(...)`).

### 7.3 Organização dos Services
- **Services Enxutos:** Nenhuma classe `*Service` deve conter construtores manuais privados de Responses ou entidades espalhados (`criarVoto`, `criarHistoricoInicial`, `carregarResponse`).
- **Métodos Privados de Edição:** Manter `private void aplicarEm(Entity entity, RequestDto request)` no final do Service.
- **Streams e Mapeamento Inline:** O agrupamento de resultados em lote (ex: `Collectors.toMap`) deve ser feito inline nos métodos de consulta e paginação.
- **Java Funcional:** Utilizar métodos idiomáticos como `.ifPresentOrElse(...)` para transições condicionais (ex: alternância de votos).

### 7.4 Injeção de Dependências, Caching & Transações
- **Injeção via Construtor:** Injeção explícita com atributos `private final` e anotação `@RequiredArgsConstructor` do Lombok.
- **Transacionalidade:**
  - Consultas e listagens puras: `@Transactional(readOnly = true)`.
  - Mutações e escritas: `@Transactional`.
- **Cache Declarativo do Spring:**
  - Tabelas de catálogo (`CategoriaService` e `StatusService`) utilizam `@Cacheable` nas consultas e `@CacheEvict(allEntries = true)` nas mutações.
- **Timeouts de Integrações HTTP:**
  - Clientes declarativos (`@HttpExchange` / ViaCEP) possuem timeouts explícitos configurados no `application.properties` (`spring.http.clients.connect-timeout=2s` e `read-timeout=4s`).

### 7.5 Padrão de Request DTOs com Enum
- **Nunca use String para campos de Enum:** Use o tipo Enum diretamente no record e anote com `@EnumValidation` para a mensagem de erro amigável.
- **Sem ternários de fallback para Enum:** Se o campo tem um valor padrão de negócio, use `@Builder.Default` na entidade ou defina no DDL — nunca no DTO.
- **Sem verificações null redundantes em campos `@NotNull`:** Se a borda valida, o Service confia.

---

## 🐳 8. Containerização com Docker (Multi-Stage Build)

Os Dockerfiles utilizam **multi-stage build** (`gradle:8.10-jdk21-alpine` → `eclipse-temurin:21-jre-alpine`) e **devem ser buildados a partir da raiz `Java-Advanced/`**:

```bash
# Build da API (a partir de Java-Advanced/)
docker build -f zelafix-api/Dockerfile -t zelafix-api:latest .

# Build do Worker (a partir de Java-Advanced/)
docker build -f zelafix-worker/Dockerfile -t zelafix-worker:latest .

# Execução da API (injete variáveis de ambiente do .env)
docker run -p 8080:8080 --env-file .env zelafix-api:latest

# Execução do Worker
docker run -p 8081:8081 --env-file .env zelafix-worker:latest
```

**Diretrizes de Segurança e Ambiente:**
- Usuário não-root (`appuser:appgroup`) — conformidade de segurança corporativa.
- Timezone `America/Sao_Paulo` — timestamps Oracle e logs consistentes com o fuso brasileiro.
- JVM 21 LTS (`eclipse-temurin:21-jre-alpine`).
- API: Geração dinâmica de chaves RSA via `openssl` no stage de build.

---

## ☁️ 9. Fluxo Git, Políticas de Branch e CI/CD em Nuvem

### Sincronização GitHub ↔ Azure Repos
O repositório possui dois remotes configurados:
- `origin`: GitHub (`Global-Solution-CitizenTech/Java-Advanced`)
- `azure`: Azure Repos (`ZelaFixGS/ZelaFix`)

### Regras de Ouro de Branching & Compliance:
1. **Branch `develop`:**
   - Branch de desenvolvimento diário.
   - Pushes no GitHub acionam o workflow `.github/workflows/mirror-to-azure.yml`, que espelha automaticamente para a branch `develop` no Azure Repos via `git push azure HEAD:develop --force`.
   - O Azure DevOps detecta o push e executa a esteira `ZelaFix-CI-CD` na branch `develop`.
2. **Branch `main` (Produção Protegida):**
   - **NÃO é permitido push direto na `main` no Azure Repos** (`TF402455`).
   - A promoção para `main` é realizada **exclusivamente via Pull Request no Azure DevOps**, exigindo:
     - Mínimo de 1 revisor com voto de aprovação.
     - Vínculo obrigatório com Work Item no Azure Boards (ex: `#169`).
   - Após a conclusão do PR no Azure Repos, a pipeline `ZelaFix-CI-CD` executa o deploy oficial nos Azure App Services:
     - `app-zelafix-561432` (API na porta 8080)
     - `app-zelafix-worker-561432` (Worker na porta 8081)

---

## 🛠️ 10. Guia Rápido de Comandos para Agentes

| Ação | Comando |
| :--- | :--- |
| **Compilar e rodar testes do worker** | `powershell -Command "cd Java-Advanced; .\gradlew :zelafix-worker:test"` |
| **Compilar e rodar testes da API** | `powershell -Command "cd Java-Advanced; .\gradlew :zelafix-api:test"` |
| **Executar suite completa de testes** | `powershell -Command "cd Java-Advanced; .\gradlew test"` |
| **Executar API localmente (porta 8080)** | `powershell -Command "cd Java-Advanced; .\gradlew :zelafix-api:bootRun"` |
| **Executar Worker localmente (porta 8081)** | `powershell -Command "cd Java-Advanced; .\gradlew :zelafix-worker:bootRun"` |
| **Build Docker da API** | `docker build -f zelafix-api/Dockerfile -t zelafix-api:latest .` (da raiz `Java-Advanced/`) |
| **Build Docker do Worker** | `docker build -f zelafix-worker/Dockerfile -t zelafix-worker:latest .` (da raiz `Java-Advanced/`) |
| **Mensageria** | RabbitMQ gerenciado no CloudAMQP; defina `SPRING_RABBITMQ_ADDRESSES` (amqps://...) no `.env` |

---

## ✅ 11. Checklist de Validação do Agente

Antes de considerar uma tarefa finalizada:
- [x] O código compila perfeitamente sem warnings críticos (`.\gradlew clean test bootJar`).
- [x] Todos os testes unitários e de integração de ambos os módulos passam (`.\gradlew test`).
- [x] Nenhum arquivo temporário de debug, log ou script avulso foi deixado no repositório.
- [x] O `zelafix-worker` é 100% stateless (sem conexões ou dependências de banco de dados).
- [x] DTOs de Response não possuem sobrecargas duplicadas e DTOs de Request não possuem mutadores de entidade.
- [x] Os Services estão enxutos e utilizam `toEntity` dos DTOs e `aplicarEm` privado.
- [x] Enums são utilizados diretamente nos DTOs com `@EnumValidation`.
- [x] Dockerfiles utilizam multi-stage build com `eclipse-temurin:21-jre-alpine` e usuário não-root.
- [x] A esteira de CI/CD e as políticas de branch do Azure Repos e GitHub Actions foram respeitadas.
- [x] A documentação arquitetural no `AGENT.md` e no `README.md` reflete estritamente a realidade da base de código.
