# 🏙️ ZelaFix Backend — Microsserviços e Mensageria Assíncrona com RabbitMQ & Spring AI
> **FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)**  
> **Disciplina:** Java Advanced  
> **Opção Distribuída Escolhida:** **Opção A — Mensageria Assíncrona com RabbitMQ** (Padrão de Arquitetura Distribuída Reativa)  
> **Deploy em Nuvem:** Azure Web Apps (Linux) via Esteira Automatizada CI/CD Azure DevOps

---

## 👥 Integrantes do Grupo (Ordem Alfabética Estrita)

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
├── gradlew / gradlew.bat             # Gradle Wrapper corporativo (Gradle 8.10)
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
│       │   │   ├── chamado/          # Ocorrências urbanas, histórico e votos
│       │   │   ├── categoria/        # Catálogo com cache declarativo (@Cacheable)
│       │   │   ├── status/           # Máquina de estados de resolução das ocorrências
│       │   │   ├── endereco/         # Normalização geográfica e integração ViaCEP
│       │   │   ├── amqp/             # Produtores RabbitMQ e Feedback Consumer da IA
│       │   │   ├── config/           # SecurityConfig, RabbitConfig, RestClientConfig
│       │   │   └── exception/        # GlobalExceptionHandler RFC 7807
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/migration/     # Flyway V1 (DDL), V2 (Seeding), V3 (Admin Inicial)
│       └── test/                     # Testes unitários e de integração com Spring Boot Test
│
└── zelafix-worker/                   # MICROSSERVIÇO 2: Worker Reativo Stateless
    ├── Dockerfile                    # Multi-stage build leve (sem JDBC/JPA, non-root)
    ├── build.gradle                  # Dependências Spring AMQP e Spring AI (Azure AI Foundry)
    └── src/
        ├── main/
        │   ├── java/fiap/com/br/zelafix/worker/
        │   │   ├── consumer/         # ChamadoAiConsumer & VotoConsumer (@RabbitListener)
        │   │   ├── producer/         # TriagemFeedbackProducer (retorno à API)
        │   │   ├── service/          # ZelaFixAiService (Spring AI ChatClient multimodal)
        │   │   ├── util/             # ImageMimeTypeDetector (Magic Bytes detector)
        │   │   └── config/           # RabbitWorkerConfig & JacksonConfig
        │   └── resources/
        │       └── application.properties
        └── test/                     # Suíte de testes unitários do consumidor de IA
```

---

## 🏛️ 2. Arquitetura Distribuída Reativa e Divisão de Responsabilidades

O ecossistema **ZelaFix** é operado por **dois microsserviços especializados** interligados por mensageria com **RabbitMQ**:

```
                              ┌──────────────────────────────────────────────┐
                              │            Client Layer (Mobile/Web)         │
                              └──────────────────────┬───────────────────────┘
                                                     │ HTTP REST / JWT Bearer (RSA)
                                                     ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│  zelafix-api (Porta 8080) — Gateway RESTful de Domínio & Persistência ACID                             │
│  - Spring Security 6 com JWT RSA (Nimbus) & RBAC (ROLE_CIDADAO, ROLE_GESTOR, ROLE_ADMIN)               │
│  - Persistência exclusiva no Banco Relacional Oracle FIAP via Spring Data JPA                          │
│  - Versionamento de banco automático com Flyway Migrations (V1 DDL, V2 Carga, V3 Admin)                │
│  - Cache declarativo de catálogo com @Cacheable/@CacheEvict (Categoria e Status)                       │
│  - Cliente declarativo HTTP com timeouts e resiliência (ViaCepService)                                 │
│  - Produtor RabbitMQ: publica ocorrências com foto obrigatória e votos no zelafix.direct               │
│  - Consumidor RabbitMQ (Feedback): consome triagens concluídas da IA e persiste no Oracle via JPA      │
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
│  - Inteligência Artificial Generativa e Multimodal com Spring AI (Azure AI Foundry / Phi-4 Multimodal) │
│  - Detecção dinâmica de MIME type via magic bytes (ImageMimeTypeDetector)                              │
│  - Agregador de votos em buffer volátil thread-safe (ConcurrentHashMap com flush periódico a cada 3s)  │
│  - 100% Stateless: ZERO CONEXÃO COM BANCO DE DADOS (sem JDBC, sem JPA, sem Oracle, sem locks)          │
│  - Produtor de retorno: publica parecer técnico estruturado em chamado.triagem-concluida.queue         │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚫 3. Desacoplamento Radical: Por que o Worker NÃO acessa o Banco?

A ausência total de dependências JPA/JDBC e conexões de banco de dados no `zelafix-worker` é um diferencial arquitetural sênior:

1. **Isolamento de Falhas e Resiliência:** O worker atua como um *pure inference worker*. Lentidões ou indisponibilidades temporárias do banco de dados não afetam o consumo da IA nem saturam o pool de conexões (HikariCP).
2. **Escalabilidade Horizontal Instantânea:** Instâncias do worker sobem em ~2 segundos e podem ser multiplicadas elasticamente sem disputar o limite de conexões simultâneas do Oracle corporativo.
3. **Padrão de Feedback Assíncrono:** O worker devolve o parecer técnico estruturado (`TriagemFeedbackMessage`) para a fila `chamado.triagem-concluida.queue`. A `zelafix-api`, única detentora legal do schema relacional, consome o feedback via `ChamadoTriagemFeedbackConsumer` e realiza a transação ACID com auditoria histórica (`HistChamado`).
4. **Absorção de Rajadas de Votos (Padrão Paredão):** O `VotoConsumer` acumula apoios em memória com `ConcurrentHashMap<Long, Integer>`. O flush periódico a cada 3 segundos (`@Scheduled`) consolida os lotes em log e libera a memória, sem estressar o banco relacional.

---

## 🔄 4. Topologia e Fluxo de Mensageria RabbitMQ (`zelafix.direct`)

Toda a comunicação assíncrona entre os dois serviços é orquestrada pela `DirectExchange` nomeada `zelafix.direct`:

| Fila | Routing Key | Produtor | Consumidor | Payload | Finalidade |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `chamado.triagem.queue` | `chamado.triagem.key` | `zelafix-api` | `zelafix-worker` | `ChamadoTriagemMessage` | Dispara a triagem multimodal com Spring AI transportando os bytes da foto. |
| `chamado.triagem-concluida.queue` | `chamado.triagem-concluida.key` | `zelafix-worker` | `zelafix-api` | `TriagemFeedbackMessage` | Devolve a prioridade, órgão responsável e parecer técnico para persistência JPA. |
| `chamado.voto.queue` | `chamado.voto.key` | `zelafix-api` | `zelafix-worker` | `VotoMessage` | Notifica apoios/votos em ocorrências para agregação de alta vazão em memória. |

- **Serialização Padronizada:** Mensagens trafegam como JSON canônico (UTF-8) com `JacksonJsonMessageConverter`, eliminando dependências frágeis de `java.io.Serializable`.

---

## 🧠 5. Triagem Inteligente Multimodal com Spring AI (`ZelaFixAiService`)

Toda ocorrência registrada na plataforma exige foto comprobatória (`BLOB NOT NULL` no banco de dados):

- **Injeção Idiomática do `ChatClient`:** Configurado com `SYSTEM_PROMPT` técnico e `SimpleLoggerAdvisor` para observabilidade de prompts no console.
- **Detecção de MIME Type por Magic Bytes (`ImageMimeTypeDetector`):** Analisa a assinatura binária dos primeiros bytes (`image/jpeg`, `image/png`, `image/webp`, `image/gif`) para anexar dinamicamente como `ByteArrayResource` no prompt multimodal.
- **Structured Output Tipado:** O retorno do LLM é automaticamente serializado para o record `TriagemIaResult`:
  ```java
  public record TriagemIaResult(
      PrioridadeChamado prioridade,         // BAIXA, MEDIA, ALTA, URGENTE
      DepartamentoGoverno orgaoResponsavel, // SECRETARIA_OBRAS, DEFESA_CIVIL, CET_TRANSITO, ILUMINACAO_PUBLICA, etc.
      String parecerIa                      // Parecer técnico objetivo (até 150 caracteres)
  ) {}
  ```

---

## 📊 6. Tabela de Conformidade com os Critérios da Global Solution (100 pts)

| Item | Critério de Avaliação | Pontos | Como Foi Atendido no Projeto |
| :---: | :--- | :---: | :--- |
| **01** | **API REST do Domínio** (mínimo 3 entidades relacionadas, CRUD e paginação) | **20 pts** | Mapeamento relacional de 10 entidades conforme `citizenTech.ddl` (`Usuario`, `Telefone`, `Chamado`, `Categoria`, `Status`, `VotoChamado`, `HistChamado`, `Endereco`, `Bairro`, `Cidade`, `Estado`). Paginação nativa com `Pageable`, ordenação dinâmica e filtros por status, categoria e busca textual. |
| **02** | **Validação & Tratamento de Erros** (`@Valid` e `@RestControllerAdvice`) | **10 pts** | Bean Validation estrito em todos os DTOs (`@NotBlank`, `@Email`, `@Size`, `@DddValidation`, `@EnumValidation`). `GlobalExceptionHandler` captura violações de contrato e regras de negócio retornando respostas padronizadas RFC 7807. |
| **03** | **Segurança com JWT & RBAC** (múltiplos papéis de usuário) | **20 pts** | Spring Security 6 / OAuth2 Resource Server com tokens JWT assinados via par de chaves RSA 2048 bits geradas automaticamente pelo build. Controle de acesso por papéis: `ROLE_CIDADAO`, `ROLE_GESTOR` e `ROLE_ADMIN`. |
| **04** | **Spring AI Integrado ao Fluxo de Negócio** | **20 pts** | Triagem multimodal via `ChatClient` no `zelafix-worker`. Consome mensagem com bytes da foto, detecta MIME type via magic bytes, classifica urgência e órgão competente com structured output tipado (`TriagemIaResult`) e publica o parecer técnico no RabbitMQ. |
| **05** | **Arquitetura Distribuída Funcional (Opção A — RabbitMQ)** | **20 pts** | 2 microsserviços autônomos (`zelafix-api` e `zelafix-worker` 100% Stateless). Mensageria bidirecional com RabbitMQ: DirectExchange `zelafix.direct`, 3 filas duráveis e agregação de votos com buffer volátil thread-safe e flush periódico em lote. |
| **06** | **Documentação OpenAPI/Swagger & Integração Mobile** | **10 pts** | Swagger interativo em `/swagger-ui.html`. Integração 100% validada com o app mobile React Native Expo (`Mobile-Application-Development`), com rotas diretas padronizadas (`/chamados`, `/usuarios`, `/usuarios/me`, `/login`). |
| **TOTAL** | **Pontuação Total da Entrega** | **100 pts** | **Solução Completa, Testada, Containerizada e com Deploy Ativo em Nuvem.** |

---

## ☁️ 7. Esteira de CI/CD e Deploy em Produção (Azure DevOps & App Services)

O projeto possui esteira unificada de Integração Contínua e Entrega Contínua (CI/CD) totalmente automatizada:

```
GitHub Push (main/develop)
        │
        ▼ (GitHub Actions: mirror-to-azure.yml)
Azure Repos (dev.azure.com/ZelaFixGS/ZelaFix)
        │
        ▼ (Trigger nativo individualCI)
Azure Pipelines (ZelaFix-CI-CD)
        │
        ├── ETAPA 1 (CI): clean test bootJar (Gradle Wrapper + JDK 21)
        ├── Publicação dos relatórios JUnit (PublishTestResults@2)
        ├── Publicação dos artefatos jar (PublishBuildArtifacts@1)
        │
        ▼ (Condição: succeeded() em main ou develop)
        └── ETAPA 2 (CD): Deploy Automático via AzureWebApp@1
                ├── app-zelafix-561432 (Porta 8080 — zelafix-api.jar)
                └── app-zelafix-worker-561432 (Porta 8081 — zelafix-worker.jar)
```

### URLs das Aplicações em Produção (Azure App Services)
- **API REST (Porta 8080):** https://app-zelafix-561432.azurewebsites.net
- **Swagger UI em Nuvem:** https://app-zelafix-561432.azurewebsites.net/swagger-ui.html
- **Worker Reativo (Porta 8081):** https://app-zelafix-worker-561432.azurewebsites.net

---

## 🐳 8. Containerização com Docker (Multi-Stage Build)

Ambos os microsserviços utilizam **Dockerfiles multi-stage build** otimizados para produção:
- **Build Stage:** `gradle:8.10-jdk21-alpine` (compilação sem daemon e geração de chaves RSA para JWT via OpenSSL).
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

## 🔑 9. Credenciais de Teste para Avaliação

O banco de dados Oracle é inicializado automaticamente com usuários de teste e dados pré-carregados via migrations versionadas do Flyway:

| Papel | Nome | E-mail | Senha | Permissões |
| :--- | :--- | :--- | :--- | :--- |
| **Cidadão** | Carlos Alberto Silva | `carlos.silva@email.com` | `Senha#123` | Criar ocorrências com foto, apoiar ocorrências com votos e gerenciar perfil. |
| **Funcionário da Prefeitura** | Eng. Roberto Santos | `roberto.gestor@subprefeitura.sp.gov.br` | `Senha#123` | Agente público: atualizar status dos chamados (`EM_ANALISE`, `EM_EXECUCAO`, `CONCLUIDO`, `REJEITADO`). |
| **Administrador Geral** | Administrador ZelaFix | `admin@zelafix.com.br` | `123mudar` | Acesso irrestrito: CRUD de usuários, chamados, categorias, status, endereços e alteração de perfis. |

---

## 🚀 10. Guia de Execução Local

### Pré-requisitos
- **Java 21 LTS** (Amazon Corretto ou Eclipse Temurin 21).
- Arquivo `.env` configurado na raiz de `Java-Advanced` (baseado em `.env.example`), apontando para o Oracle FIAP e para a instância gerenciada do **RabbitMQ no CloudAMQP** (`SPRING_RABBITMQ_ADDRESSES=amqps://...`).

### Executando localmente via Gradle
```bash
# Terminal 1 — Iniciar a API REST (Porta 8080)
.\gradlew :zelafix-api:bootRun

# Terminal 2 — Iniciar o Worker Reativo (Porta 8081)
.\gradlew :zelafix-worker:bootRun
```

### Executando os Testes Automatizados
```bash
# Executar todos os testes de ambos os módulos
.\gradlew test

# Executar testes individualmente por módulo
.\gradlew :zelafix-api:test
.\gradlew :zelafix-worker:test
```

---

## 📱 11. Integração com o App Mobile (React Native Expo)

O app mobile em `Mobile-Application-Development` conecta-se diretamente à `zelafix-api`:
- **Android Emulator:** `http://10.0.2.2:8080`
- **iOS Simulator / Web:** `http://localhost:8080`
- **Ambiente em Nuvem (Produção):** `https://app-zelafix-561432.azurewebsites.net`

Contratos alinhados para retornar o objeto atualizado do chamado em operações de apoio (`POST /chamados/{id}/apoiar`), paginação transparente (`content[]`), autenticação JWT persistida no SecureStore e renderização direta de fotos em base64.

---

## 🤖 12. Declaração Obrigatória de Uso de IA Generativa

Em conformidade com as diretrizes do regulamento da Global Solution 2026/2:
- **Ferramentas de IA:** Google Antigravity / Gemini 3.8 / Claude Sonnet 4.6 no apoio ao design arquitetural, estrutura Gradle Multi-Project e geração de suítes de testes automatizados.
- **Spring AI:** O microsserviço `zelafix-worker` utiliza a biblioteca oficial `spring-ai-starter-model-openai` (Spring AI 2.0.1) consumindo o modelo de visão computacional multimodal `phi-4-multimodal` (Phi-4 Multimodal via Azure AI Foundry) para triagem técnica automatizada, classificação de urgência e parecer técnico em prol do cidadão e da gestão pública.
