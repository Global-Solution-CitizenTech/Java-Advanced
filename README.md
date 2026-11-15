# 🏙️ ZelaFix Backend — Microsserviços e Mensageria Assíncrona com RabbitMQ & Spring AI
> **FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)**  
> **Disciplina:** Java Advanced  
> **Opção Distribuída Escolhida:** **Opção A — Mensageria Assíncrona com RabbitMQ** (Padrão de Arquitetura Distribuída Orientada a Eventos)  
> **Deploy em Nuvem:** Azure Web Apps (Linux) via Esteira Automatizada CI/CD Azure DevOps

| Link Rápido | URL |
|---|---|
| **Repositório GitHub (Java)** | https://github.com/Global-Solution-CitizenTech/Java-Advanced |
| **Repositório GitHub (DevOps)** | https://github.com/Global-Solution-CitizenTech/DevOps-Tools-Cloud-Computing |
| **Projeto no Azure DevOps** | https://dev.azure.com/ZelaFixGS/ZelaFix |
| **API em Produção (Azure Web App)** | https://app-zelafix.azurewebsites.net |
| **Worker IA em Produção (Azure Web App)** | https://app-zelafix-worker.azurewebsites.net |
| **Swagger UI Interativo (Produção)** | https://app-zelafix.azurewebsites.net/swagger-ui.html |
| **OpenAPI Docs JSON (Produção)** | https://app-zelafix.azurewebsites.net/v3/api-docs |
| **Actuator Health API (Produção)** | https://app-zelafix.azurewebsites.net/actuator/health |
| **Actuator Health Worker (Produção)** | https://app-zelafix-worker.azurewebsites.net/actuator/health |
| **Swagger UI (Local)** | http://localhost:8080/swagger-ui.html |
| **Coleção Oficial de Automação (Postman QA)** | https://github.com/Global-Solution-CitizenTech/Compliance-Quality-Assurance-Tests |

## 🔗 Vídeo Pitch e Demonstração (Global Solution 2)

* **Vídeo Pitch (até 5 min):** *(Link do YouTube será inserido após gravação)*
* **Vídeo de Demonstração Técnica:** *(Link do YouTube será inserido após gravação)*

---

## 👥 Integrantes do Grupo

| RM | Nome Completo | Turma |
| :---: | :--- | :---: |
| **RM561432** | Enzo Okuizumi Miranda de Souza | 2TDSPG |
| **RM563428** | Gustavo Keiji Okada | 2TDSPG |
| **RM566422** | Lucas Barros Gouveia | 2TDSPG |
| **RM562290** | Luna de Carvalho Guimarães | 2TDSPG |
| **RM564836** | Milton Jakson de Sousa Marcelino | 2TDSPG |

---

## 📂 1. Estrutura do Repositório & Arquitetura Gradle Multi-Project

### Por que adotamos Gradle Multi-Project? *(Justificativa Técnica Corporativa)*
> *"Adotamos a arquitetura de **Gradle Multi-Project**, que é a boa prática corporativa para microsserviços que compartilham o mesmo ecossistema e ciclo de release. Ela nos permite ter controle centralizado das versões do Spring Boot (4.1.1), Java (21 LTS) e repositórios Maven, compilar e testar toda a esteira de CI/CD de forma atômica no Azure DevOps em um único comando (`call gradlew.bat clean test bootJar`), e ao mesmo tempo manter `zelafix-api` e `zelafix-worker` como artefatos completamente desacoplados, independentes e isolados em tempo de execução."*

### Árvore de Diretórios do Projeto
```text
Java-Advanced/
├── .github/
│   └── workflows/
│       └── mirror-to-azure.yml      # Espelhamento automático GitHub ↔ Azure Repos
├── azure-pipelines.yml               # Pipeline unificado de CI/CD (Azure DevOps)
├── build.gradle                      # Gradle root: controle centralizado de plugins
├── settings.gradle                   # Declaração dos submódulos: zelafix-api e zelafix-worker
├── gradle.properties                 # Configurações de JVM e encoding UTF-8
├── gradlew / gradlew.bat             # Gradle Wrapper corporativo (Gradle 9.8.2)
├── AGENT.md                          # Guia operacional da arquitetura e convenções
│
├── zelafix-api/                      # MICROSSERVIÇO 1: Gateway REST & Persistência
│   ├── Dockerfile                    # Multi-stage build (Temurin 21 JRE Alpine, non-root)
│   ├── build.gradle                  # Dependências JPA, Oracle, Flyway, Security, RabbitMQ
│   └── src/
│       ├── main/
│       │   ├── java/fiap/com/br/zelafix/
│       │   │   ├── auth/             # Autenticação JWT RSA, Login & UserDetailsService
│       │   │   ├── usuario/          # Gestão de munícipes e perfis (Cidadão, Gestor, Admin)
│       │   │   ├── chamado/          # Ocorrências urbanas (Chamado, TriagemPendenteStore, dto/, prioridade/, orgao/)
│       │   │   ├── categoria/        # Catálogo com cache declarativo (@Cacheable)
│       │   │   ├── endereco/         # Normalização geográfica e integração ViaCEP (@HttpExchange)
│       │   │   ├── historico/        # Auditoria de transições (HistChamado) e máquina de estados (status/)
│       │   │   ├── voto/             # Controle de apoio comunitário (VotoChamado)
│       │   │   ├── queue/            # Produtores RabbitMQ e Feedback Consumer da IA
│       │   │   ├── validation/       # Anotações e validadores customizados (@DddValidation)
│       │   │   ├── util/             # Utilitários (ImageMimeTypeDetector)
│       │   │   ├── config/           # SecurityConfig, RabbitConfig, SwaggerConfig
│       │   │   └── exception/        # GlobalExceptionHandler
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/     # Flyway V1 (DDL 13 tabelas), V2 (Domínios Base + Admin), V3 (Dados Iniciais)
│       └── test/                     # Testes unitários e de controller; suíte E2E Python em tests/
│
└── zelafix-worker/                   # MICROSSERVIÇO 2: Worker Stateless de Processamento Assíncrono
    ├── Dockerfile                    # Multi-stage build leve (sem JDBC/JPA, non-root)
    ├── build.gradle                  # Dependências Spring AMQP e Spring AI (LLM Multimodal + Tool Calling)
    └── src/
        ├── main/
        │   ├── java/fiap/com/br/zelafix/worker/
        │   │   ├── ai/               # ZelaFixAiService (Fail-Fast), TriagemNormativaTools (@Tool), dto/, enums/ e util/
        │   │   ├── consumer/         # ChamadoAiConsumer & VotoConsumer (@RabbitListener)
        │   │   ├── queue/            # DTOs de mensageria (ChamadoTriagemMessage, TriagemFeedbackMessage)
        │   │   └── config/           # RabbitWorkerConfig
        │   └── resources/
        │       └── application.properties
        └── test/                     # Suíte de testes unitários do consumidor de IA e RabbitMQ
```
---

## ☁️ 2. Arquitetura em Nuvem & Esteira CI/CD (Azure DevOps & App Services)

<p align="center">
  <img src="./docs/gs2-zelafix-diagrama.png" alt="Arquitetura em Nuvem Microsoft Azure & Esteira CI/CD ZelaFix" width="100%" />
</p>

---

## 🏛️ 3. Arquitetura Distribuída Orientada a Eventos e Divisão de Responsabilidades

O ecossistema **ZelaFix** é operado por **dois microsserviços especializados** interligados por mensageria com **RabbitMQ**:

```
                              ┌──────────────────────────────────────────────┐
                              │            Client Layer (Mobile/Web)         │
                              └──────────────────────┬───────────────────────┘
                                                     │ HTTP REST / JWT Bearer (RSA)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-api (Porta 8080) — Gateway RESTful de Domínio & Persistência ACID                             │
│  - Spring Security com JWT RSA (Nimbus) & RBAC (ROLE_CIDADAO, ROLE_GESTOR, ROLE_ADMIN)                 │
│  - Persistência no Oracle FIAP via Spring Data JPA / Hibernate 7.4 com @CreationTimestamp declarativo  │
│  - Java 21 Project Loom: Virtual Threads ativas (spring.threads.virtual.enabled=true) para I/O leve    │
│  - Versionamento de banco automático com Flyway Migrations (V1 DDL, V2 Domínios + Admin, V3 Seed)      │
│  - Cache declarativo de catálogo com @Cacheable/@CacheEvict (Categoria, Status, Prioridade, Órgão)     │
│  - Cliente HTTP declarativo com @HttpExchange / @GetExchange e @ImportHttpServices (ViaCepService)     │
│  - Produtor RabbitMQ + DeferredResult: publica endereço + foto no zelafix.direct e aguarda triagem     │
│  - Consumidor RabbitMQ (Feedback): recusa imagens sem persistir ou grava chamado com histórico duplo   │
│    conectado via saveAll (PENDENTE inicial de abertura pelo munícipe + EM_ANALISE triado pela IA)      │
│  - Consumidor RabbitMQ (Prioridade): escala prioridade no Oracle sob mobilização popular comunitária   │
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
│  - 100% Stateless: ZERO CONEXÃO COM BANCO DE DADOS (sem JDBC, sem JPA, sem Oracle, sem locks)          │
│  - Produtor de retorno: publica parecer técnico e escalonamento comunitário no zelafix.direct          │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚫 4. Desacoplamento Radical: Por que o Worker NÃO acessa o Banco?

A ausência total de dependências JPA/JDBC e conexões de banco de dados no `zelafix-worker` é um diferencial arquitetural sênior:

1. **Isolamento de Falhas e Resiliência:** O worker atua como um *pure inference worker*. Lentidões ou indisponibilidades temporárias do banco de dados não afetam o consumo da IA nem saturam o pool de conexões (HikariCP).
2. **Escalabilidade Horizontal Instantânea:** Instâncias do worker inicializam rapidamente de forma desacoplada e podem ser multiplicadas elasticamente sem disputar o limite de conexões simultâneas do Oracle corporativo.
3. **Triagem Assíncrona com `DeferredResult` e Histórico Duplo Conectado:** Na criação do chamado (`POST /chamados` `multipart/form-data` com `cep`, `numero`, `referencia`, `rua` e `foto`), o cidadão envia apenas endereço e evidência fotográfica. A API registra um `correlationId` em `TriagemPendenteStore` (timeout de `60s`) e publica os bytes na fila. O worker analisa a imagem e devolve `TriagemFeedbackMessage` para `chamado.triagem-concluida.queue`. Se a imagem for aprovada (`ocorrenciaValida = true`), a `zelafix-api` persiste o chamado em transação ACID salvando o histórico duplo conectado via `saveAll` (`PENDENTE` inicial pelo munícipe + `EM_ANALISE` triado pela IA) e conclui a requisição com `201 Created`; se recusada (`recusado = true`), rejeita a requisição com `400 Bad Request` sem gravar lixo no Oracle.
4. **Buffer de Rajada e Escalonamento Comunitário Orientado a Eventos:** O apoio individual do munícipe (`POST /chamados/{id}/apoiar`) é registrado de forma síncrona e atômica na API para garantir consistência ACID e resposta instantânea ao usuário mobile (`apoiadoPorMim: true`). Imediatamente após a gravação, a API publica um evento em `chamado.voto.queue`. O `VotoConsumer` no worker absorve essas rajadas em um buffer thread-safe em memória (`ConcurrentHashMap`), evitando que o banco sofra com rotinas contínuas de polling e recálculo de prioridades. A cada 3 segundos (`@Scheduled`), o worker avalia se o chamado atingiu os limiares comunitários ($\ge 3$ votos $\rightarrow$ `ALTA`, $\ge 5$ votos $\rightarrow$ `URGENTE`). Ao atingir, publica `PrioridadeEscaladaMessage` para a API, que executa o escalonamento estritamente ascendente no Oracle (nunca rebaixando prioridades urgentes) com registro histórico em `HistChamado`.

### ⚠️ Limitações Conhecidas e Evolução Arquitetural (`TriagemPendenteStore`)
O mecanismo de triagem assíncrona síncrona-ao-cliente via `DeferredResult` utiliza o `TriagemPendenteStore` baseado em `ConcurrentHashMap` local na JVM da `zelafix-api`.
- **Comportamento Atual:** Funciona com perfeição em topologias com instância única da API (como no deployment atual do Azure App Service).
- **Cenário Multi-Instância (Cluster Horizontal):** Caso a API seja escalada horizontalmente em múltiplas instâncias atrás de um Load Balancer genérico (Round-Robin), a resposta do worker consumida em uma réplica B pode não encontrar o `DeferredResult` registrado na memória da réplica A.
- **Evolução Corporativa Planejada:** Para ambientes de altíssima escala multi-instância, a arquitetura prevê a transição para:
  1. *Session Affinity / Sticky Sessions* no Ingress Controller/Gateway; ou
  2. Notificação bidirecional via **WebSockets / SSE (Server-Sent Events)** orquestrada com **Redis Pub/Sub**, permitindo que qualquer réplica da API entregue o parecer ao cliente conectado.

---

## 🔄 5. Topologia e Fluxo de Mensageria RabbitMQ (`zelafix.direct`)

Toda a comunicação assíncrona entre os dois serviços é orquestrada pela `DirectExchange` nomeada `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Payload | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Dispara a triagem multimodal com Spring AI transportando `correlationId`, endereço e bytes da foto. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Devolve parecer da IA (ou recusa/contingência) correlacionado para persistência condicional e conclusão do `DeferredResult`. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica apoios/votos em ocorrências para agregação de alta vazão em memória. |
| `chamado.prioridade-escalada.queue` | `chamado.prioridade-escalada.key` | `zelafix-worker` | `zelafix-api` | `PrioridadeEscaladaMessage` | Notifica a API para elevar a prioridade do chamado no Oracle quando o volume de apoios atinge os limiares comunitários. |

- **Serialização Padronizada:** Mensagens trafegam como JSON canônico (UTF-8) com `JacksonJsonMessageConverter`, eliminando dependências frágeis de `java.io.Serializable`.

---

## 🧠 6. Triagem Inteligente Multimodal com Spring AI (`ZelaFixAiService`)

Toda ocorrência registrada na plataforma exige foto comprobatória (`BLOB NOT NULL` no banco de dados):

- **Injeção Idiomática & Fail-Fast:** `ChatClient` configurado com `SYSTEM_PROMPT`, `SimpleLoggerAdvisor`, `defaultTools(TriagemNormativaTools)` e estratégia **Fail-Fast** imediata (sem retries em chamadas síncronas de usuário para garantir tempo de resposta de 3s a 5s no app mobile e acionar contingência graciosa `PENDENTE` instantaneamente em caso de falha).
- **Tool Calling Stateless (`TriagemNormativaTools`):** Expõe `@Tool consultarNormaMunicipal` (decretos PMSP e órgão padrão) e `@Tool calcularPrazoSla` (`America/Sao_Paulo`, `12h`/`24h`/`48h`/`72h`), acionadas apenas quando `ocorrenciaValida = true`.
- **Detecção de MIME Type por Magic Bytes (`ImageMimeTypeDetector`):** Analisa a assinatura binária dos primeiros bytes (`image/jpeg`, `image/png`, `image/webp`, `image/gif`) para anexar dinamicamente como `ByteArrayResource` no prompt multimodal parametrizado (`{bairro}`).
- **Structured Output Validado + Proteção UTF-8 (`EntityParamSpec::validateSchema` + `comLimitesOracle()`):** O retorno do LLM é validado contra o JSON Schema e truncado por code-points UTF-8 (`<= 100` bytes no título e `<= 1000` bytes na descrição) para evitar estouro de coluna `VARCHAR2(... BYTE)` no Oracle:
  ```java
  public record TriagemIaResult(
      boolean ocorrenciaValida,             // true apenas para problemas reais em espaço público urbano
      String titulo,                        // Título curto e objetivo (máx. 100 bytes UTF-8)
      String descricao,                     // Descrição técnica e riscos (máx. 1000 bytes UTF-8)
      CategoriaSugerida categoriaSugerida,  // Categoria oficial da zeladoria urbana
      PrioridadeChamado prioridade,         // BAIXA, MEDIA, ALTA, URGENTE
      DepartamentoGoverno orgaoResponsavel  // SECRETARIA_OBRAS, CET_TRANSITO, ILUMINACAO_PUBLICA, DEFESA_CIVIL, SUBPREFEITURA, SANEAMENTO_BASICO
  ) {}
  ```
- **Triagem em Contingência via RabbitMQ:** Caso o provedor de IA esteja indisponível ou com cota excedida (HTTP 429), o worker aciona imediatamente a contingência assíncrona sem retries desnecessários, devolvendo o chamado com status `PENDENTE`, prioridade `MEDIA` e órgão `SUBPREFEITURA` para triagem manual da equipe técnica municipal.

---

## 📡 7. Catálogo dos Principais Endpoints da API REST

A API do ZelaFix segue rigorosamente o padrão RESTful, retornando DTOs canônicos e tratando erros segundo a **RFC 7807 (Problem Details)**:

| Método | Endpoint | Permissão | Descrição |
| :--- | :--- | :--- | :--- |
| `POST` | `/login` | Público | Autentica munícipe/gestor e retorna par de tokens JWT assinados com RSA Nimbus (2048-bit). |
| `POST` | `/usuarios` | Público | Cadastro de novo cidadão na plataforma com validações Jakarta e hash BCrypt. |
| `GET` | `/usuarios/me` | Autenticado | Retorna dados cadastrais e permissões do usuário logado. |
| `GET` | `/chamados` | Autenticado | Listagem paginada (`Pageable`) de chamados com filtros e enriquecimento em lote. |
| `GET` | `/chamados/{id}` | Autenticado | Detalhamento do chamado com histórico completo de auditoria e cálculo de apoios. |
| `GET` | `/chamados/{id}/foto` | Público | Retorna o binário da evidência fotográfica da ocorrência (`image/jpeg`, `image/png`). |
| `POST` | `/chamados` | Autenticado | Abertura de ocorrência (`multipart/form-data`: endereço + foto). Dispara triagem via `DeferredResult`. |
| `PUT` | `/chamados/{id}` | Autor / `ADMIN` | Edição dos dados da ocorrência via método idiomático `aplicarEm`. |
| `DELETE` | `/chamados/{id}` | Autor / `ADMIN` | Exclusão física com remoção limpa de históricos e votos associados. |
| `PATCH` | `/chamados/{id}/status` | `GESTOR` / `ADMIN` | Atualização de status operacional (`EM_EXECUCAO`, `CONCLUIDO`, etc.) com parecer técnico. |
| `POST` | `/chamados/{id}/apoiar` | Autenticado | Alterna apoio comunitário (toggle), persistindo voto no Oracle e publicando evento AMQP. |
| `GET` | `/chamados/estatisticas` | Autenticado | Retorna métricas consolidadas da plataforma (chamados por status, totais e apoios). |
| `GET` | `/swagger-ui.html` | Público | Documentação interativa Swagger OpenAPI 3.0 para exploração e testes em tempo real. |

---

## 🤖 8. Declaração de Uso de Inteligência Artificial Generativa

Em cumprimento às diretrizes éticas e acadêmicas da Global Solution FIAP:

- **Modelos Utilizados no Desenvolvimento:** **Gemini 3.8 Flash**, **Gemini 4 Hydrogen** e **Claude Sonnet 5.5** (Anthropic).
- **Finalidade e Escopo de Uso:** Os modelos atuaram estritamente como ferramentas de assistência (*AI Pair Programming*) nas seguintes frentes:
  1. *Prototipação e Refatoração de Código:* Sugestão de padrões idiomáticos modernos do Java 21 LTS (records, virtual threads, pattern matching) e DTOs enxutos sem *boilerplate*.
  2. *Suíte de Testes Automatizados:* Geração de esqueletos e cenários de borda para testes unitários com JUnit 5 e Mockito.
  3. *Revisão de Conformidade Técnica:* Apoio na revisão cruzada entre os requisitos arquiteturais e os artefatos de documentação.
- **Responsabilidade Humana:** Toda a concepção arquitetural, modelagem relacional em 3FN no Oracle Database, implementação do par de chaves RSA Nimbus para JWT, topologia de mensageria com RabbitMQ, prompts e *tool calling* com Spring AI foram concebidos, revisados, ajustados e integralmente homologados pelos integrantes humanos do grupo. Nenhuma decisão de design ou código foi aplicada sem curadoria técnica.

---

## 🐳 9. Containerização com Docker (Multi-Stage Build)

Ambos os microsserviços utilizam **Dockerfiles multi-stage build** otimizados para produção:
- **Build Stage:** `eclipse-temurin:21-jdk-alpine` executando o Gradle Wrapper oficial do projeto (`./gradlew`) para garantir paridade total de compilação sem divergências de versão.
- **Runtime Stage:** `eclipse-temurin:21-jre-alpine` (imagem base ultraleve de produção).
- **Segurança:** Execução obrigatória com usuário não-root (`appuser:appgroup`).
- **Timezone:** Configurado para `America/Sao_Paulo` (garante integridade de auditoria com o Oracle).

### Comandos de Build e Execução Docker (a partir da raiz `Java-Advanced/`)
```bash
# 1. Build da imagem da API
docker build -f zelafix-api/Dockerfile -t zelafix-api:latest .

# 2. Build da imagem do Worker
docker build -f zelafix-worker/Dockerfile -t zelafix-worker:latest .

# 3. Execução dos containers injetando variáveis de ambiente
docker run -d -p 8080:8080 --env-file .env --name zelafix-api zelafix-api:latest
docker run -d -p 8081:8081 --env-file .env --name zelafix-worker zelafix-worker:latest
```

---

## 🔑 10. Credenciais de Teste

O banco de dados Oracle é inicializado automaticamente com usuários de teste via migrations do Flyway:

| Papel | Nome | E-mail | Senha | Permissões |
| :--- | :--- | :--- | :--- | :--- |
| **Cidadão** | Enzo Okuizumi | `enzo.okuizumi@gmail.com` | `123mudar` | Criar ocorrências com foto + endereço, apoiar com votos, editar próprios dados e consultar histórico. |
| **Gestor Público** | Eng. Roberto Santos | `roberto.gestor@subprefeitura.sp.gov.br` | `Senha#123` | Agente público: atualizar status dos chamados (`PENDENTE`, `EM_ANALISE`, `EM_EXECUCAO`, `CONCLUIDO`, `REJEITADO`). |
| **Gestor Público** | Coordenador Lucas Mendes | `lucas.zeladoria@subprefeitura.sp.gov.br` | `Senha#123` | Agente público: acompanhar triagens, atualizar status e registrar observações técnicas. |
| **Administrador Geral** | Administrador Geral | `admin@zelafix.com.br` | `123mudar` | Acesso irrestrito: CRUD de usuários, chamados e endereços, e alteração de perfis de acesso. |

---

## 🚀 11. Guia de Execução Local (Clone do Zero)

Siga este passo a passo para executar o projeto a partir de uma clonagem limpa:

### 1. Pré-requisitos
* **Java 21 LTS** instalado (`java -version`).
* **Docker & Docker Desktop** ativos (para RabbitMQ local, caso não utilize CloudAMQP).

### 2. Configurar o arquivo `.env`
Na raiz da pasta `Java-Advanced`, copie o modelo de exemplo:
```bash
# Windows PowerShell
Copy-Item .env.example .env
```
Abra o `.env` e confirme as credenciais do **Oracle Database** (FIAP ou container local), **RabbitMQ** (`SPRING_RABBITMQ_ADDRESSES`) e o **Provedor de LLM Multimodal via Spring AI** (`AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`).

### 3. Iniciar o RabbitMQ (caso use broker local via Docker)
```bash
docker run -d --name zelafix-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```
*(O painel administrativo do RabbitMQ estará acessível em `http://localhost:15672` com usuário `guest` e senha `guest`).*

### 4. Compilar e Testar a Aplicação
Execute o build atômico do Gradle para garantir que todos os testes unitários e de integração passem:
```bash
# Windows
.\gradlew.bat clean test bootJar

# Linux / macOS
./gradlew clean test bootJar
```

### 5. Iniciar os Microsserviços
Abra dois terminais na pasta `Java-Advanced`:
```bash
# Terminal 1 — Iniciar a API REST (Porta 8080)
.\gradlew.bat :zelafix-api:bootRun

# Terminal 2 — Iniciar o Worker de Processamento Assíncrono (Porta 8081)
.\gradlew.bat :zelafix-worker:bootRun
```

* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **Health Check da API:** `http://localhost:8080/actuator/health`
* **Health Check do Worker:** `http://localhost:8081/actuator/health`
