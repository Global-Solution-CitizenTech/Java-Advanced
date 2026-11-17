# 🏙️ ZelaFix Backend — Serviços Desacoplados e Mensageria Assíncrona com RabbitMQ & Spring AI
> **FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)**  
> **Disciplina:** Java Advanced  
> **Opção Distribuída Escolhida:** **Opção A — Mensageria Assíncrona com RabbitMQ**  
> **Deploy em Nuvem:** Azure Web Apps (Linux) via Esteira CI/CD no Azure DevOps

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

## 📂 1. Estrutura do Repositório & Gradle Multi-Project

### Por que usamos Gradle Multi-Project?
Organizamos o repositório como um projeto multi-módulo no Gradle para centralizar as versões de dependências (Spring Boot 4.1.1 e Java 21) em um único `build.gradle` raiz e rodar todos os testes e builds da esteira de CI com um só comando (`gradlew clean test bootJar`). Ao mesmo tempo, o Gradle gera dois arquivos `.jar` separados (`zelafix-api` e `zelafix-worker`), permitindo implantar, reiniciar ou escalar cada serviço de forma independente.

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
├── gradlew / gradlew.bat             # Gradle Wrapper (Gradle 9.8.2)
├── AGENT.md                          # Guia operacional da arquitetura e convenções
│
├── zelafix-api/                      # SERVIÇO 1: API REST & Persistência
│   ├── Dockerfile                    # Multi-stage build (Temurin 21 JRE Alpine, non-root)
│   ├── build.gradle                  # Dependências JPA, Oracle, Flyway, Security, RabbitMQ
│   └── src/
│       ├── main/
│       │   ├── java/fiap/com/br/zelafix/
│       │   │   ├── auth/             # Autenticação JWT RSA, Login & UserDetailsService
│       │   │   ├── usuario/          # Gestão de munícipes e perfis (Cidadão, Gestor, Admin)
│       │   │   ├── chamado/          # Ocorrências urbanas (Chamado, TriagemPendenteStore, dto/, prioridade/, orgao/)
│       │   │   ├── categoria/        # Catálogo com cache (@Cacheable)
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
└── zelafix-worker/                   # SERVIÇO 2: Worker Stateless de Processamento Assíncrono
    ├── Dockerfile                    # Multi-stage build (sem JDBC/JPA, non-root)
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

## 🗄️ Modelagem de Dados Relacional & Lógica (Oracle Database)

O banco relacional do **ZelaFix** foi modelado na **Terceira Forma Normal (3FN)** para evitar redundância de dados, manter o histórico de mudanças de status e armazenar as fotos das ocorrências em coluna `BLOB`:

### Modelo Lógico de Dados (Entidades e Relacionamentos)
<p align="center">
  <img src="./docs/logical.png" alt="Modelo Lógico - ZelaFix Oracle Database" width="90%" />
</p>

### Modelo Relacional de Dados (Tabelas, Chaves Primárias, Estrangeiras e Constraints)
<p align="center">
  <img src="./docs/relational.png" alt="Modelo Relacional - ZelaFix Oracle Database" width="90%" />
</p>

---

## 🏛️ 3. Divisão de Responsabilidades e Fluxo de Mensageria

O sistema é dividido em **dois serviços** que se comunicam de forma assíncrona via **RabbitMQ**:

```
                              ┌──────────────────────────────────────────────┐
                              │            Client Layer (Mobile/Web)         │
                              └──────────────────────┬───────────────────────┘
                                                     │ HTTP REST / JWT Bearer (RSA)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-api (Porta 8080) — API REST & Persistência no Banco                                           │
│  - Spring Security com JWT RSA (Nimbus) & RBAC (ROLE_CIDADAO, ROLE_GESTOR, ROLE_ADMIN)                 │
│  - Persistência no Oracle via Spring Data JPA / Hibernate 7.4 com @CreationTimestamp                   │
│  - Virtual Threads do Java 21 ativas (spring.threads.virtual.enabled=true)                             │
│  - Versionamento de schema automático com Flyway Migrations (V1 DDL, V2 Domínios + Admin, V3 Seed)     │
│  - Cache em memória de catálogo com @Cacheable/@CacheEvict (Categoria, Status, Prioridade, Órgão)      │
│  - Cliente HTTP declarativo com @HttpExchange / @GetExchange e @ImportHttpServices (ViaCepService)     │
│  - Produtor RabbitMQ + DeferredResult: envia endereço + foto para o zelafix.direct e aguarda triagem   │
│  - Consumidor RabbitMQ (Feedback): descarta imagens inválidas ou grava chamado com histórico duplo     │
│    via saveAll (PENDENTE na abertura pelo cidadão + EM_ANALISE após triagem da IA)                     │
│  - Consumidor RabbitMQ (Prioridade): eleva prioridade no Oracle conforme apoios comunitários           │
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
│  zelafix-worker (Porta 8081) — Worker Stateless de IA e Agregação                                      │
│  - Consumidor assíncrono (RabbitMQ @RabbitListener com Virtual Threads)                                │
│  - Classificação de imagens e texto com Spring AI (LLM Multimodal + Tool Calling)                      │
│  - Estratégia Fail-Fast com fallback para status PENDENTE via ChamadoAiConsumer                        │
│  - Detecção de MIME type via magic bytes e redimensionamento JPEG (ImageMimeTypeDetector)              │
│  - Agregador de votos em buffer em memória com limiares (>=3 ALTA, >=5 URGENTE)                        │
│  - Stateless: SEM CONEXÃO COM BANCO DE DADOS (sem JDBC, sem JPA, sem Oracle)                           │
│  - Produtor de retorno: publica resultado da triagem e escalonamento no zelafix.direct                 │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚫 4. Por que o Worker não acessa o Banco de Dados?

Removemos o JPA, o JDBC e qualquer conexão direta ao Oracle no `zelafix-worker` por quatro motivos práticos:

1. **Isolamento de falhas:** Chamadas externas para APIs de IA levam alguns segundos e podem oscilar. Como o worker não abre transações no banco, uma lentidão no provedor de IA não prende conexões do pool (HikariCP) nem trava as consultas da API principal.
2. **Controle de conexões no Oracle:** O banco acadêmico possui limite baixo de sessões simultâneas. Deixando apenas a `zelafix-api` conectar ao banco, podemos subir novas instâncias do worker sem disputar conexões com o Oracle.
3. **Validação antes de gravar (`DeferredResult`):** No `POST /chamados` (`multipart/form-data` com `cep`, `numero`, `referencia`, `rua` e `foto`), a API guarda a requisição em espera no `TriagemPendenteStore` (timeout de `60s`) e envia a imagem para a fila. O worker analisa a foto e devolve o parecer na fila `chamado.triagem-concluida.queue`. Se a foto for de um problema urbano válido (`ocorrenciaValida = true`), a API grava o chamado e dois registros de histórico via `saveAll` (`PENDENTE` na abertura + `EM_ANALISE` da triagem) e responde `201 Created`. Se a foto for inválida (`recusado = true`), a API responde `400 Bad Request` sem salvar registros inúteis no banco.
4. **Agrupamento de votos em rajada:** Quando vários usuários apoiam o mesmo chamado ao mesmo tempo (`POST /chamados/{id}/apoiar`), a API grava o voto no banco e publica um evento em `chamado.voto.queue`. O `VotoConsumer` no worker acumula esses eventos em um `ConcurrentHashMap` em memória e verifica os totais a cada 3 segundos (`@Scheduled`). Quando o chamado atinge $\ge 3$ votos (`ALTA`) ou $\ge 5$ votos (`URGENTE`), o worker envia uma única mensagem de escalonamento para a API atualizar o Oracle, evitando recálculos repetidos no banco a cada clique.

### ⚠️ Limitação Conhecida (`TriagemPendenteStore`)

O `TriagemPendenteStore` guarda os objetos `DeferredResult` em um `ConcurrentHashMap` na memória local da JVM da `zelafix-api`.

* **Cenário Atual:** Funciona normalmente com uma instância da API (como está configurado hoje no Azure App Service).
* **Cenário com Múltiplas Instâncias:** Se a API rodar em duas ou mais réplicas atrás de um Load Balancer sem afinidade de sessão, a resposta do worker pode cair na réplica B enquanto a requisição HTTP do cliente está aguardando na memória da réplica A, causando timeout.
* **Próximo Passo para Múltiplas Réplicas:** Caso o projeto precise escalar a API horizontalmente, a solução é habilitar *Sticky Sessions* no Load Balancer ou trocar o `DeferredResult` por **Server-Sent Events (SSE) / WebSockets** sincronizados via **Redis Pub/Sub**.

---

## 🔄 5. Topologia e Filas do RabbitMQ (`zelafix.direct`)

Toda a troca de mensagens entre os dois serviços passa pela `DirectExchange` `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Payload | Finalidade |
| --- | --- | --- | --- | --- | --- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Envia `correlationId`, endereço e bytes da foto para análise do Spring AI. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Retorna o parecer da IA (ou recusa/contingência) para a API concluir o `DeferredResult`. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica novos apoios para contagem em memória no worker. |
| `chamado.prioridade-escalada.queue` | `chamado.prioridade-escalada.key` | `zelafix-worker` | `zelafix-api` | `PrioridadeEscaladaMessage` | Solicita à API que eleve a prioridade do chamado no Oracle ao atingir o limite de apoios. |

* **Serialização:** As mensagens são serializadas em JSON (UTF-8) com `JacksonJsonMessageConverter`.

---

## 🧠 6. Triagem Multimodal com Spring AI (`ZelaFixAiService`)

Toda ocorrência exige o envio de uma foto (`BLOB NOT NULL` no banco):

* **Configuração e Fail-Fast:** O `ChatClient` usa `SYSTEM_PROMPT`, `SimpleLoggerAdvisor` e `defaultTools(TriagemNormativaTools)`. Como o usuário está aguardando a resposta na tela do app, desativamos retries automáticos nas chamadas ao LLM: se a API de IA falhar ou demorar, o worker aciona o fallback imediatamente para manter a resposta entre 3s e 5s.
* **Tool Calling (`TriagemNormativaTools`):** Disponibiliza duas ferramentas locais para o modelo consultar apenas quando `ocorrenciaValida = true`: `@Tool consultarNormaMunicipal` (decretos de São Paulo e órgão responsável) e `@Tool calcularPrazoSla` (fuso `America/Sao_Paulo`, prazos de `12h`, `24h`, `48h` ou `72h`).
* **Verificação de Tipo de Imagem (`ImageMimeTypeDetector`):** Lê os primeiros bytes do arquivo (*magic bytes*) para identificar se é `image/jpeg`, `image/png`, `image/webp` ou `image/gif` antes de montar o `ByteArrayResource` enviado no prompt.
* **Saída Estruturada e Limite de Bytes UTF-8 (`EntityParamSpec::validateSchema` + `comLimitesOracle()`):** O retorno do modelo é validado contra o JSON Schema e truncado por contagem de bytes UTF-8 (`<= 100` bytes no título e `<= 1000` bytes na descrição) para não estourar o limite das colunas `VARCHAR2(... BYTE)` do Oracle:
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
* **Fallback em Caso de Indisponibilidade:** Se o provedor de IA retornar erro ou limite de cota (HTTP 429), o worker devolve o chamado com status `PENDENTE`, prioridade `MEDIA` e órgão `SUBPREFEITURA`, permitindo que a ocorrência seja salva e triada manualmente depois.

---

## 📡 7. Catálogo dos Principais Endpoints da API REST

A API retorna DTOs padronizados e centraliza o tratamento de erros em `@RestControllerAdvice` (`GlobalExceptionHandler`) com payload JSON estruturado (`timestamp`, `status`, `error`, `message`, `path` e lista de `erros` de validação):

| Método | Endpoint | Permissão | Descrição |
| --- | --- | --- | --- |
| `POST` | `/login` | Público | Autentica usuário e retorna token JWT assinado com chave RSA (2048-bit). |
| `POST` | `/usuarios` | Público | Cadastro de novo cidadão com validação Jakarta e senha criptografada com BCrypt. |
| `GET` | `/usuarios/me` | Autenticado | Retorna dados cadastrais e perfil do usuário logado. |
| `GET` | `/chamados` | Autenticado | Listagem paginada (`Pageable`) de chamados com filtros. |
| `GET` | `/chamados/{id}` | Autenticado | Detalhes do chamado, histórico de status e contagem de apoios. |
| `GET` | `/chamados/{id}/foto` | Público | Retorna os bytes da foto (redimensionada em JPEG até 1080px se > 200 KB) com `Cache-Control: public, max-age=86400`. |
| `POST` | `/chamados` | Autenticado | Abertura de ocorrência (`multipart/form-data`: endereço + foto) com triagem via `DeferredResult`. |
| `PUT` | `/chamados/{id}` | Autor / `GESTOR` / `ADMIN` | Edição dos dados da ocorrência via método `aplicarEm`. |
| `DELETE` | `/chamados/{id}` | Autor / `GESTOR` / `ADMIN` | Exclusão do chamado junto com seus históricos e votos. |
| `PATCH` | `/chamados/{id}/status` | `GESTOR` / `ADMIN` | Atualização de status operacional (`EM_EXECUCAO`, `CONCLUIDO`, etc.) com parecer técnico. |
| `POST` | `/chamados/{id}/apoiar` | Autenticado | Adiciona ou remove apoio comunitário (toggle), grava no Oracle e publica evento na fila. |
| `GET` | `/chamados/estatisticas` | Autenticado | Retorna totais de chamados agrupados por status e apoios. |
| `GET` | `/swagger-ui.html` | Público | Documentação interativa Swagger OpenAPI 3.0. |

---

## 🤖 8. Declaração de Uso de Inteligência Artificial Generativa

Em cumprimento às diretrizes da Global Solution FIAP:

* **Modelos Utilizados no Desenvolvimento:** **Gemini 3.8 Flash**, **Gemini 4 Hydrogen** e **Claude Sonnet 5.5** (Anthropic).
* **Finalidade e Escopo de Uso:** Os modelos foram usados como apoio durante o desenvolvimento nas seguintes tarefas:
1. *Refatoração e sintaxe Java 21:* Sugestões de uso de `records`, *pattern matching* e redução de código repetitivo em DTOs.
2. *Testes Automatizados:* Geração inicial de cenários de teste unitário com JUnit 5 e Mockito.
3. *Revisão de Documentação:* Conferência entre os requisitos do edital e as seções deste README.


* **Responsabilidade Humana:** A arquitetura do sistema, a modelagem do banco Oracle, a configuração do JWT RSA, a topologia de filas do RabbitMQ, os prompts e ferramentas do Spring AI foram projetados, revisados e testados pelos integrantes do grupo.

---

## 🐳 9. Containerização com Docker (Multi-Stage Build)

Os dois serviços possuem **Dockerfiles multi-stage** para gerar imagens enxutas:

* **Build Stage:** `eclipse-temurin:21-jdk-alpine` executando `./gradlew` para compilar os `.jar` com a mesma versão do Gradle configurada no repositório.
* **Runtime Stage:** `eclipse-temurin:21-jre-alpine` contendo apenas o JRE necessário para rodar a aplicação.
* **Segurança:** Os containers rodam com usuário sem privilégios de root (`appuser:appgroup`).
* **Timezone:** Configurado como `America/Sao_Paulo` para manter os timestamps alinhados com o banco Oracle.

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

O banco de dados Oracle é populado com contas de demonstração via migrations do Flyway:

| Papel | Nome | E-mail | Senha | Permissões |
| --- | --- | --- | --- | --- |
| **Cidadão** | Enzo Okuizumi | `enzo.okuizumi@gmail.com` | `123mudar` | Criar ocorrências com foto + endereço, apoiar chamados, editar próprios dados e consultar histórico. |
| **Gestor Público** | Eng. Roberto Santos | `roberto.gestor@subprefeitura.sp.gov.br` | `Senha#123` | Atualizar status dos chamados (`PENDENTE`, `EM_ANALISE`, `EM_EXECUCAO`, `CONCLUIDO`, `REJEITADO`). |
| **Gestor Público** | Coordenador Lucas Mendes | `lucas.zeladoria@subprefeitura.sp.gov.br` | `Senha#123` | Acompanhar triagens, atualizar status e registrar observações técnicas. |
| **Administrador Geral** | Administrador Geral | `admin@zelafix.com.br` | `123mudar` | Gerenciar usuários, chamados, endereços e perfis de acesso. |

---

## 🚀 11. Guia de Execução Local (Clone do Zero)

Siga este passo a passo para rodar o projeto localmente:

### 1. Pré-requisitos
* **Java 21 LTS** instalado (`java -version`).
* **Docker & Docker Desktop** ativos (para subir o RabbitMQ local, caso não utilize CloudAMQP).

### 2. Configurar o arquivo `.env`

Na raiz da pasta `Java-Advanced`, copie o modelo de exemplo:

```bash
# Windows PowerShell
Copy-Item .env.example .env

```

Abra o `.env` e preencha as credenciais do **Oracle Database** (FIAP ou container local), **RabbitMQ** (`SPRING_RABBITMQ_ADDRESSES`, ex.: `amqp://guest:guest@localhost:5672` para o container local do passo 3) e do **Provedor de LLM Multimodal via Spring AI** (`AI_BASE_URL`, `AI_API_KEY`, `AI_MODEL`). Em ambientes em nuvem que hibernam por inatividade (ex.: Azure F1), configure também `WORKER_URL` na API para habilitar o *wake-up* assíncrono do worker.

### 3. Iniciar o RabbitMQ (caso use broker local via Docker)

```bash
docker run -d --name zelafix-rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

```

*(O painel do RabbitMQ ficará disponível em `http://localhost:15672` com usuário `guest` e senha `guest`).*

### 4. Compilar e Testar a Aplicação

Execute o build do Gradle para rodar os testes e empacotar os dois serviços:

```bash
# Windows
.\gradlew.bat clean test bootJar

# Linux / macOS
./gradlew clean test bootJar
```

### 5. Iniciar os Serviços
Abra dois terminais na pasta `Java-Advanced`:
```bash
# Windows PowerShell — Terminal 1 (API REST - Porta 8080) e Terminal 2 (Worker - Porta 8081)
.\gradlew.bat :zelafix-api:bootRun
.\gradlew.bat :zelafix-worker:bootRun

# Linux / macOS — Terminal 1 (API REST - Porta 8080) e Terminal 2 (Worker - Porta 8081)
./gradlew :zelafix-api:bootRun
./gradlew :zelafix-worker:bootRun
```

* **Swagger UI:** `http://localhost:8080/swagger-ui.html`
* **Health Check da API:** `http://localhost:8080/actuator/health`
* **Health Check do Worker:** `http://localhost:8081/actuator/health`
