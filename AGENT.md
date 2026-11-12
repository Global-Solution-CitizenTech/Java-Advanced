# 🤖 Guia Operacional do Agente — ZelaFix Backend (Java Advanced)
> **Padrão:** Clean Architecture & Ponytail Standard  
> **Projeto:** FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)  
> **Disciplina:** Java Advanced  
> **Opção de Distribuição:** Opção A — Mensageria Assíncrona com RabbitMQ (Padrão Paredão)

---

## 🏛️ 1. Arquitetura do Sistema e Divisão de Responsabilidades

O backend do ecossistema **ZelaFix** é estruturado em **dois microsserviços autônomos e desacoplados** que operam de forma cooperativa através do broker de mensageria **RabbitMQ**:

```
                              ┌──────────────────────────────────────────────┐
                              │            Client Layer (Mobile/Web)         │
                              └──────────────────────┬───────────────────────┘
                                                     │ HTTP REST / JWT
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-api (Porta 8080)                                                                              │
│  - Gateway RESTful de domínio e autenticação                                                           │
│  - Spring Security 6 com JWT RSA (Nimbus) & RBAC (ROLE_CIDADAO, ROLE_GESTOR, ROLE_ADMIN)               │
│  - Persistência exclusiva no Banco Relacional Oracle FIAP via Spring Data JPA                          │
│  - Validação estrita na borda (Jakarta Bean Validation nos Request DTOs + @DddValidation)              │
│  - Tratamento global de erros padronizado RFC 7807 (GlobalExceptionHandler)                            │
│  - Caching declarativo de catálogo com @Cacheable/@CacheEvict (Categoria e Status)                     │
│  - Timeouts HTTP configurados no cliente declarativo ViaCepService                                     │
│  - Produtor RabbitMQ: publica ocorrências com foto obrigatória e votos no zelafix.direct               │
│  - Consumidor RabbitMQ: consome retornos de triagem IA e persiste no Oracle via TriagemFeedbackMessage │
└───────────────────────┬────────────────────────────────────────────────────▲───────────────────────────┘
                        │                                                    │
                        │ chamado.triagem.key                                │ chamado.triagem-concluida.key
                        │ chamado.voto.key                                   │
                        ▼                                                    │
┌────────────────────────────────────────────────────────────────────────────┴───────────────────────────┐
│  RabbitMQ Broker (Porta 5672) — Exchange: zelafix.direct (DirectExchange)                              │
│  - Fila: chamado.triagem.queue (Durable)                                                               │
│  - Fila: chamado.triagem-concluida.queue (Durable)                                                     │
│  - Fila: chamado.voto.queue (Durable)                                                                  │
└───────────────────────┬────────────────────────────────────────────────────────────────────────────────┘
                        │
                        │ Consumo Assíncrono
                        ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-worker (Porta 8081) — Stateless Heavy Worker                                                  │
│  - Consumidor assíncrono de alta vazão (RabbitMQ @RabbitListener)                                      │
│  - Inteligência Artificial Generativa e Multimodal com Spring AI 2.0.1 (Groq LLM / Qwen 3.8 27B)       │
│  - Triagem técnica multimodal obrigatória: ChamadoAiConsumer -> ZelaFixAiService -> TriagemIaResult    │
│  - Suporte a visão computacional com detecção de MIME type via magic bytes (ImageMimeTypeDetector)     │
│  - Agregador de votos em buffer volátil concorrente (ConcurrentHashMap com flush periódico)            │
│  - ZERO CONEXÃO COM BANCO DE DADOS: 100% Stateless, sem JDBC, sem JPA, sem Oracle, sem locks           │
│  - Produtor de feedback: devolve parecer técnico para chamado.triagem-concluida.queue                  │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚫 2. Ausência Total de Banco de Dados no `zelafix-worker`

Uma das decisões arquiteturais mais críticas do projeto é a **completa ausência de banco de dados relacional ou transacional no microsserviço `zelafix-worker`**:

1. **Desacoplamento e Isolamento de Falhas:**
   - O worker atua como um *pure compute / inference worker*. Se a infraestrutura de banco de dados sofrer lentidão ou indisponibilidade momentânea, o processamento de inferência da IA continua fluindo e enfileirado no RabbitMQ sem provocar saturação de pool de conexões (HikariCP) ou deadlocks.
2. **Escalabilidade Horizontal Instantânea:**
   - Como o worker é estritamente *stateless*, novas instâncias do `zelafix-worker` podem ser inicializadas instantaneamente em contêineres sem preocupações com consumo de conexões simultâneas da licença do Oracle FIAP.
3. **Fluxo Bidirecional via Mensageria (Feedback Pattern):**
   - O worker **não escreve diretamente nas tabelas**. Quando conclui a inferência técnica via LLM, o worker empacota o resultado em um payload JSON (`TriagemFeedbackMessage`) e o publica de volta na fila `chamado.triagem-concluida.queue`.
   - A `zelafix-api`, que é a detentora legal e única proprietária do schema relacional, consome o feedback via `ChamadoTriagemFeedbackConsumer` e realiza a transação ACID no Oracle com auditoria histórica (`HistChamado`).
4. **Agregação Volátil de Votos (Padrão Paredão):**
   - O `VotoConsumer` armazena deltas de apoios em memória utilizando `ConcurrentHashMap<Long, Integer>`. O flush periódico a cada 3 segundos (`@Scheduled`) consolida os lotes em log e libera a memória, permitindo absorção de rajadas sem bater no banco.

---

## 🔄 3. Topologia e Fluxo de Mensageria RabbitMQ (`zelafix.direct`)

Toda a comunicação assíncrona entre os dois serviços é orquestrada por uma única `DirectExchange` nomeada `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Tipo de Payload | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Dispara a triagem técnica inteligente com Spring AI levando obrigatoriamente a foto da ocorrência em bytes. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Devolve o parecer da IA (prioridade, órgão competente e parecer técnico) para que a API atualize o banco de dados. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica apoios/votos em ocorrências para agregação em lote em alta vazão. |

### Serialização e Ausência de `Serializable`
- Todas as mensagens utilizam **Jackson JSON Converter** (`JacksonJsonMessageConverter("*")`) com `converter.setAlwaysConvertToInferredType(true)`.
- É **estritamente proibido** o uso de `java.io.Serializable` nos DTOs de mensageria. As mensagens transitam como JSON canônico (UTF-8), garantindo legibilidade, rastreabilidade via RabbitMQ Management e interoperabilidade poliglota.

---

## 🧠 4. Spring AI e Triagem Inteligente Multimodal Obrigatória (`ZelaFixAiService`)

No ZelaFix, todo chamado registrado na plataforma requer foto da ocorrência (`BLOB NOT NULL` no banco de dados):

- **Injeção Idiomática do `ChatClient`:**
  - Injeção direta de `ChatClient.Builder` no construtor.
  - Configuração do `SYSTEM_PROMPT` padrão e `SimpleLoggerAdvisor` para observabilidade de prompts no console.
- **Fluxo Multimodal Estrito:**
  - A imagem da ocorrência (`byte[] foto`) é transportada no payload de `ChamadoTriagemMessage` diretamente para o `zelafix-worker`.
  - `ChamadoAiConsumer` delega a análise de forma direta sem ramificações sem foto:
    ```java
    TriagemIaResult resultado = aiService.analisarChamado(
            message.categoria(),
            message.titulo(),
            message.descricao(),
            message.bairro(),
            message.foto()
    );
    ```
- **Detecção Automática de MimeType (`ImageMimeTypeDetector`):**
  - Analisa os *magic numbers* dos primeiros bytes da imagem (`image/jpeg`, `image/png`, `image/webp`, `image/gif`) para anexar dinamicamente como `ByteArrayResource` no `ChatClient.prompt().user(u -> u.media(...))`.
- **Structured Output Tipado:**
  - Retorno mapeado automaticamente para o record `TriagemIaResult`:
    ```java
    public record TriagemIaResult(
        PrioridadeChamado prioridade,         // BAIXA, MEDIA, ALTA, URGENTE
        DepartamentoGoverno orgaoResponsavel, // SECRETARIA_OBRAS, DEFESA_CIVIL, CET_TRANSITO, etc.
        String parecerIa                      // Parecer técnico objetivo (até 150 caracteres)
    ) {}
    ```

---

## 🧹 5. Convenções de Código Limpo e Ponytail Standard

Para manter o código sustentável, seguro e harmonioso, todos os módulos obedecem às seguintes regras:

### 5.1 Fronteira de Confiança (Validação na Borda)
- **Bean Validation no Controller:** Todo payload HTTP de entrada é validado na borda via `@Valid` e anotações nos Request DTOs (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@DddValidation`, etc.).
- **Proibição de Código Defensivo Redundante:** Não polua Services, mappers ou métodos de domínio com checagens nulas de campos que já foram validados na borda.
- **Modelagem Fiel ao DDL:** Não adicionar campos fantasmas que não existem nas tabelas do banco (ex: `dt_nascimento` não existe na tabela `usuario`).

### 5.2 Java Records, DTOs Puros e Métodos de Conversão
- **Java Records:** Todos os DTOs (Request, Response e Queue Messages) são declarados como `public record`.
- **Sem Sobrecargas Desnecessárias:**
  - Os DTOs de Response devem conter unicamente o factory method canônico:
    - `public static XResponse fromEntity(Entity e)`
    - `ChamadoResponse.fromEntity(Chamado c, String status, long totalVotos, boolean apoiado, List<HistChamado> historicos)` (recebendo a lista de históricos e montando internamente a resposta).
    - `HistoricoResponse.fromEntity(HistChamado h)`
    - `EstatisticasResponse.of(...)`
- **Request DTOs:** Não devem possuir métodos mutadores de entidades (ex: `applyTo`). A mutação de entidades para edição pertence a métodos privados `aplicarEm` nos respectivos Services.
- **Conversão DTO -> Entidade:** Feita exclusivamente através de métodos `toEntity(...)` dentro do DTO de Request ou mensagem de fila (ex: `ChamadoRequest.toEntity(...)`, `ChamadoStatusRequest.toEntity(...)`, `VotoMessage.toEntity(...)`, `TriagemFeedbackMessage.toEntity(...)`).

### 5.3 Organização dos Services
- **Services Enxutos:** Nenhuma classe `*Service` deve conter construtores manuais privados de Responses ou entidades espalhados (`criarVoto`, `criarHistoricoInicial`, `carregarResponse`).
- **Métodos Privados de Edição:** Manter `private void aplicarEm(Entity entity, RequestDto request)` no final do Service.
- **Streams e Mapeamento Inline:** O agrupamento de resultados em lote (ex: `Collectors.toMap`) deve ser feito inline nos métodos de consulta e paginação.
- **Java Funcional:** Utilizar métodos idiomáticos como `.ifPresentOrElse(...)` para transições condicionais (ex: alternância de votos).

### 5.4 Injeção de Dependências, Caching & Transações
- **Injeção via Construtor:** Injeção explícita com atributos `private final` e anotação `@RequiredArgsConstructor` do Lombok.
- **Transacionalidade:**
  - Consultas e listagens puras: `@Transactional(readOnly = true)`.
  - Mutações e escritas: `@Transactional`.
- **Cache Declarativo do Spring:**
  - Tabelas de catálogo (`CategoriaService` e `StatusService`) utilizam `@Cacheable` nas consultas e `@CacheEvict(allEntries = true)` nas mutações.
- **Timeouts de Integrações HTTP:**
  - Clientes declarativos (`@HttpExchange` / ViaCEP) possuem timeouts explícitos configurados no `application.properties` (`spring.http.clients.connect-timeout=2s` e `read-timeout=4s`).

### 5.5 Padrão de Request DTOs com Enum
- **Nunca use String para campos de Enum:** Use o tipo Enum diretamente no record e anote com `@EnumValidation` para a mensagem de erro amigável.
- **Sem ternários de fallback para Enum:** Se o campo tem um valor padrão de negócio, use `@Builder.Default` na entidade ou defina no DDL — nunca no DTO.
- **Sem verificações null redundantes em campos `@NotNull`:** Se a borda valida, o Service confia.

---

## 🐳 6. Docker — Build e Execução

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

**Características dos containers:**
- Usuário não-root (`appuser:appgroup`) — conformidade de segurança.
- Timezone `America/Sao_Paulo` — timestamps Oracle corretos.
- JVM 21 LTS — única versão suportada (`eclipse-temurin:21-jre-alpine`).
- API: RSA keys geradas via `openssl` no stage de build (JWT stateless).
- Worker: sem Oracle, sem JDBC, sem JPA — apenas RabbitMQ + Spring AI.

---

## 🛠️ 7. Guia Rápido de Comandos para Agentes

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

## ✅ 8. Checklist de Validação do Agente

Antes de considerar uma tarefa finalizada:
- [x] O código compila perfeitamente sem warnings críticos.
- [x] Todos os testes unitários de ambos os módulos passam (`.\gradlew test`).
- [x] Nenhum arquivo temporário de debug ou script avulso foi deixado no repositório.
- [x] O `zelafix-worker` é 100% stateless (sem conexões ou drivers de banco de dados).
- [x] DTOs de Response não possuem sobrecargas duplicadas e DTOs de Request não possuem mutadores de entidade.
- [x] Os Services estão enxutos e utilizam `toEntity` dos DTOs e `aplicarEm` privado.
- [x] Enums usados diretamente nos DTOs — sem `String` intermediário e sem ternários de fallback.
- [x] Dockerfiles utilizam multi-stage build com `eclipse-temurin:21-jre-alpine` e usuário não-root.
- [x] A documentação arquitetural reflete estritamente a realidade da base de código.
