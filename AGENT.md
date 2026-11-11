# 🤖 Guia Operacional do Agente — ZelaFix Backend (Java Advanced)
> **Padrão:** Clyvo Challenge 4 / Clean Architecture & Ponytail Standard  
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
│  - Validação estrita na borda (Jakarta Bean Validation nos Request DTOs)                               │
│  - Tratamento global de erros padronizado RFC 7807 (GlobalExceptionHandler)                           │
│  - Produtor RabbitMQ: publica ocorrências e votos no zelafix.direct                                    │
│  - Consumidor RabbitMQ: consome retornos de triagem IA e persiste no Oracle                           │
└───────────────────────┬────────────────────────────────────────────────────────▲───────────────────────┘
                        │                                                        │
                        │ chamado.triagem.key                                    │ chamado.triagem-concluida.key
                        │ chamado.voto.key                                       │
                        ▼                                                        │
┌────────────────────────────────────────────────────────────────────────────────┴───────────────────────┐
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
│  - Inteligência Artificial Generativa e Multimodal com Spring AI 2.0.1 (OpenAI / Groq LLM)             │
│  - Triagem técnica automatizada: ZelaFixAiService -> TriagemIaResult (Structured Output)                │
│  - Suporte a visão multimodal (LLM Vision) com detecção de MIME type (ImageMimeTypeDetector)           │
│  - Agregador de votos em buffer volátil concorrente (ConcurrentHashMap com flush periódico)            │
│  - ZERO CONEXÃO COM BANCO DE DADOS: 100% Stateless, sem JDBC, sem JPA, sem Oracle, sem locks          │
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
   - A `zelafix-api`, que é a detentora legal e única proprietária do schema relacional, consome o feedback via `TriagemFeedbackConsumer` e realiza a transação ACID no Oracle com auditoria histórica (`HistChamado`).
4. **Agregação Volátil de Votos (Padrão Paredão):**
   - O `VotoConsumer` armazena deltas de apoios em memória utilizando `ConcurrentHashMap<Long, Integer>`. O flush periódico a cada 3 segundos (`@Scheduled`) consolida os lotes em log e libera a memória, permitindo absorção de rajadas sem bater no banco.

---

## 🔄 3. Topologia e Fluxo de Mensageria RabbitMQ (`zelafix.direct`)

Toda a comunicação assíncrona entre os dois serviços é orquestrada por uma única `DirectExchange` nomeada `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Tipo de Payload | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Dispara a triagem técnica inteligente com Spring AI logo após o munícipe registrar uma nova ocorrência. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Devolve o parecer da IA (prioridade, órgão competente e parecer técnico) para que a API atualize o banco de dados. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica apoios/votos em ocorrências para agregação em lote em alta vazão. |

### Serialização e Ausência de `Serializable`
- Todas as mensagens utilizam **Jackson JSON Converter** (`JacksonJsonMessageConverter("*")`) com `converter.setAlwaysConvertToInferredType(true)`.
- É **estritamente proibido** o uso de `java.io.Serializable` nos DTOs de mensageria. As mensagens transitam como JSON canônico (UTF-8), garantindo legibilidade, rastreabilidade via RabbitMQ Management e interoperabilidade poliglota.

---

## 🧠 4. Spring AI e Triagem Inteligente Multimodal (`ZelaFixAiService`)

O serviço `ZelaFixAiService` no worker encapsula a comunicação com a API LLM (Groq / OpenAI):

- **Injeção Idiomática do `ChatClient`:**
  - Injeção direta de `ChatClient.Builder` no construtor.
  - Configuração do `SYSTEM_PROMPT` padrão e `SimpleLoggerAdvisor` para observabilidade de prompts no console.
- **Suporte Híbrido: Visão Multimodal & Triagem Textual:**
  - `analisarChamado(String categoria, String titulo, String descricao, String bairro)`: para análise quando não há imagem anexa.
  - `analisarChamado(String categoria, String titulo, String descricao, String bairro, byte[] fotoBytes)`: para triagem multimodal quando o cidadão anexa foto da ocorrência.
  - **Detecção de MimeType com `ImageMimeTypeDetector`:** Analisa os magic numbers do header dos bytes para inferir `image/jpeg`, `image/png`, `image/webp` ou `image/gif`, anexando como `ByteArrayResource` no `ChatClient.prompt().user(u -> u.media(...))`.
- **Structured Output Tipado:**
  - Retorno mapeado automaticamente para o record `TriagemIaResult`:
    ```java
    public record TriagemIaResult(
        PrioridadeChamado prioridade,        // BAIXA, MEDIA, ALTA, URGENTE
        DepartamentoGoverno orgaoResponsavel, // SUBPREFEITURA, DEFESA_CIVIL, CET_TRANSITO, etc.
        String parecerIa                     // Parecer técnico objetivo (até 150 caracteres)
    ) {}
    ```

---

## 🧹 5. Convenções de Código Limpo e Padrão Ponytail

Para manter o código sustentável, seguro e legível por humanos e agentes de IA, todos os módulos devem obedecer às seguintes regras:

### 5.1 Fronteira de Confiança (Validação na Borda)
- **Bean Validation no Controller:** Todo payload HTTP de entrada é validado na borda via `@Valid` e anotações nos Request DTOs (`@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@DddValidation`, etc.).
- **Proibição de Código Defensivo Redundante:** Não polua Services, mappers ou métodos de domínio com `if (dto.campo() == null || dto.campo().isBlank())` quando o atributo já possui validação no DTO. Confie plenamente no contrato validado.
- **Onde o `null` é Legítimo:** Apenas em atributos que são genuinamente opcionais na modelagem de negócio (ex: complemento de logradouro, filtros opcionais em queries).

### 5.2 Java Records e Factory Methods em DTOs
- **Java Records:** Todos os DTOs (Request, Response e Queue Messages) devem ser declarados como `public record`.
- **Response DTOs:** Devem conter métodos estáticos de fábrica idiomáticos:
  - `public static XResponse from(XEntity entity)` (ou `fromEntity`)
  - `public static EstatisticasResponse of(...)`
  - Acesso direto aos getters das entidades relacionadas obrigatórias, sem cascatas de validação nula defensiva.
- **Request DTOs:** Podem expor métodos de conveniência para aplicar alterações nas entidades JPA, como `request.applyTo(chamado, categoria, endereco)`.

### 5.3 Injeção de Dependências & Transações
- **Construtores Explícitos / Lombok:** Injeção via construtor com atributos `private final` e `@RequiredArgsConstructor`. Jamais utilize `@Autowired` em campos (`Field Injection`).
- **Transacionalidade:**
  - Consultas e listagens puras: `@Transactional(readOnly = true)`.
  - Escritas, mutações e exclusões: `@Transactional`.

### 5.4 Tratamento Centralizado de Exceções (RFC 7807)
- Todas as exceções dispostas nos fluxos de negócio lançam exceções semânticas (`ResourceNotFoundException`, `BusinessException`, `IllegalStateException`).
- O `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepta essas exceções e devolve um `ProblemDetail` estruturado com o status HTTP correspondente (400, 404, 401, 403, 500), sem necessidade de blocos `try-catch` dispersos nos Controllers.

---

## 🛠️ 6. Guia Rápido de Comandos para Agentes

| Ação | Comando |
| :--- | :--- |
| **Compilar e rodar testes do worker** | `cmd /c "cd Java-Advanced && gradlew.bat :zelafix-worker:test"` |
| **Compilar e rodar testes da API** | `cmd /c "cd Java-Advanced && gradlew.bat :zelafix-api:test"` |
| **Executar suite completa de testes** | `cmd /c "cd Java-Advanced && gradlew.bat test"` |
| **Executar API localmente (porta 8080)** | `cmd /c "cd Java-Advanced && gradlew.bat :zelafix-api:bootRun"` |
| **Executar Worker localmente (porta 8081)** | `cmd /c "cd Java-Advanced && gradlew.bat :zelafix-worker:bootRun"` |
| **Subir RabbitMQ via Docker Compose** | `docker compose -f Java-Advanced/docker-compose.yml up -d` |

---

## ✅ 7. Checklist de Validação do Agente

Antes de marcar uma tarefa como concluída e passar a responsabilidade para o `@verifier`:
- [x] O código compila perfeitamente sem warnings críticos.
- [x] Todos os testes unitários do módulo passam (`gradlew :<modulo>:test`).
- [x] Nenhum arquivo temporário de debug ou teste de script solto foi deixado na árvore de diretórios.
- [x] O `zelafix-worker` não possui dependências de banco de dados, nem arquivos de configuração apontando para Oracle/Hikari, e nenhuma classe implementando `Serializable`.
- [x] A documentação arquitetural reflete estritamente o código fonte implementado.
