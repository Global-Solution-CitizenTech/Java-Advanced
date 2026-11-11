# 🏙️ ZelaFix Backend — Microsserviços e Mensageria Assíncrona com RabbitMQ & Spring AI
> **FIAP — Global Solution 2026/2 | CitizenTech & Cidades Sustentáveis (ODS 11)**  
> **Disciplina:** Java Advanced  
> **Opção Distribuída Escolhida:** **Opção A — Mensageria Assíncrona com RabbitMQ** (Padrão Projeto *Paredão*)

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

## 🏛️ 1. Arquitetura Distribuída do Sistema

O backend foi arquitetado em **dois microsserviços autônomos e desacoplados** que se comunicam de forma assíncrona através de mensageria com **RabbitMQ**:

1. **`zelafix-api` (Gateway REST, Persistência & Mensageria — Porta 8080):**
   - API RESTful do domínio consumida pelo aplicativo mobile (**React Native Expo**) e painel web administrativo.
   - Autenticação e autorização via **Spring Security com tokens Bearer JWT assinados por par de chaves RSA**.
   - Mapeamento relacional exclusivo com **Spring Data JPA** e paginação (`Pageable`) sobre o schema corporativo Oracle (`citizenTech.ddl`).
   - Cache declarativo de catálogo (`@Cacheable` / `@CacheEvict` em `Categoria`, `Status` e `ViaCepService`) e timeouts HTTP configurados.
   - Validações de entrada na borda com **Jakarta Bean Validation** (`@Valid`, `@DddValidation`, `@EnumValidation`) e tratamento centralizado de exceções (`@RestControllerAdvice`).
   - **Produtor RabbitMQ:** Publica eventos de abertura de ocorrências com foto obrigatória (`chamado.triagem.key`) e votos/apoios da comunidade (`chamado.voto.key`).
   - **Consumidor RabbitMQ (`ChamadoTriagemFeedbackConsumer`):** Escuta a fila de retorno `chamado.triagem-concluida.queue` e persiste o parecer técnico da IA no histórico do chamado (`HistChamado`).

2. **`zelafix-worker` (Consumidor Assíncrono 100% Stateless & Heavy Worker — Porta 8081):**
   - Microsserviço de processamento pesado em segundo plano, **100% Stateless e sem qualquer conexão com banco de dados** (zero JDBC/JPA), garantindo isolamento de falhas e escalabilidade horizontal.
   - **Triagem Inteligente Multimodal via Spring AI:** Escuta a fila `chamado.triagem.queue`, detecta automaticamente o MIME type da foto via *magic numbers* (`ImageMimeTypeDetector`) e aciona o modelo multimodal **`qwen/qwen3.8-27b` (via Groq API / Spring AI)** para análise visual da foto da ocorrência, classificação de urgência (`PrioridadeChamado`), identificação do órgão municipal competente (`DepartamentoGoverno`) e emissão de parecer técnico estruturado (`TriagemIaResult`), devolvendo o resultado na fila `chamado.triagem-concluida.queue`.
   - **Agregador de Votos (`VotoConsumer`):** Escuta a fila `chamado.voto.queue`, acumula deltas de votos em memória utilizando `ConcurrentHashMap` thread-safe para absorver picos de tráfego (Padrão *Paredão*), e consolida os lotes a cada 3 segundos com `@Scheduled(fixedDelay = 3000) flush()`.

```mermaid
flowchart TD
    subgraph ClientLayer ["Camada Cliente"]
        Mobile["📱 Mobile App (React Native Expo)"]
        Swagger["🌐 Swagger UI / Web Gestor"]
    end

    subgraph Service1 ["Microsserviço 1: zelafix-api (Porta 8080)"]
        AuthController["AuthController (/login)"]
        UsuarioController["UsuarioController (/usuarios, /usuarios/me)"]
        ChamadoController["ChamadoController (/chamados, /chamados/{id}/apoiar)"]
        Security["Spring Security JWT (RSA Nimbus)"]
        JPA1["Spring Data JPA + Cache (Oracle Database FIAP)"]
        ChamadoPublisher["ChamadoEventProducer"]
        VotoPublisher["VotoEventProducer"]
        FeedbackConsumer["ChamadoTriagemFeedbackConsumer (@RabbitListener)"]
    end

    subgraph RabbitMQCluster ["RabbitMQ Message Broker (Portas 5672 / 15672)"]
        Exchange["zelafix.direct (Direct Exchange)"]
        QueueTriagem["chamado.triagem.queue (Durable)"]
        QueueFeedback["chamado.triagem-concluida.queue (Durable)"]
        QueueVoto["chamado.voto.queue (Durable)"]
    end

    subgraph Service2 ["Microsserviço 2: zelafix-worker (Porta 8081 — 100% Stateless)"]
        ChamadoAiConsumer["ChamadoAiConsumer (@RabbitListener)"]
        SpringAI["Spring AI ChatClient (Groq / Qwen 3.8 27B Vision)"]
        FeedbackProducer["TriagemFeedbackProducer"]
        VotoConsumer["VotoConsumer (@RabbitListener)"]
        VoteBuffer["ConcurrentHashMap Buffer (Padrão Paredão)"]
        Scheduler["@Scheduled(fixedDelay = 3000) Flush Lote"]
    end

    subgraph Database ["Banco de Dados Relacional (Oracle FIAP)"]
        Tables["Tabelas Relacionais (citizenTech.ddl)
        - chamado
        - hist_status
        - voto_chamado
        - usuario"]
    end

    Mobile -->|REST HTTP + JWT Bearer| ChamadoController
    Mobile -->|REST HTTP| AuthController
    Mobile -->|REST HTTP| UsuarioController
    Swagger -->|REST HTTP| ChamadoController

    ChamadoController --> Security
    ChamadoController --> JPA1
    ChamadoController -->|Publica chamado + foto| ChamadoPublisher
    ChamadoController -->|Publica voto| VotoPublisher

    ChamadoPublisher -->|routingKey: chamado.triagem.key| Exchange
    VotoPublisher -->|routingKey: chamado.voto.key| Exchange

    Exchange --> QueueTriagem
    Exchange --> QueueVoto

    QueueTriagem -->|Consome evento + foto| ChamadoAiConsumer
    ChamadoAiConsumer --> SpringAI
    SpringAI --> FeedbackProducer
    FeedbackProducer -->|routingKey: chamado.triagem-concluida.key| Exchange
    Exchange --> QueueFeedback
    QueueFeedback -->|Consome parecer IA| FeedbackConsumer
    FeedbackConsumer -->|Registra HistChamado| JPA1

    QueueVoto -->|Consome voto| VotoConsumer
    VotoConsumer --> VoteBuffer
    Scheduler -->|Consolida deltas a cada 3s| VoteBuffer

    JPA1 <--> Database
```

---

## 📊 2. Tabela de Conformidade com os Critérios da Global Solution (100 pts)

| Item | Critério de Avaliação | Pontos | Como Foi Atendido no Projeto |
| :---: | :--- | :---: | :--- |
| **01** | **API REST do Domínio** (mínimo 3 entidades relacionadas, CRUD e paginação) | **20 pts** | Mapeamento relacional de 10 entidades conforme `citizenTech.ddl` (`Usuario`, `Telefone`, `Chamado`, `Categoria`, `Status`, `VotoChamado`, `HistChamado`, `Endereco`, `Bairro`, `Cidade`, `Estado`). Suporte a paginação com `Pageable`, ordenação dinâmica e filtros por status, categoria e busca textual. |
| **02** | **Validação & Tratamento de Erros** (`@Valid` e `@RestControllerAdvice`) | **10 pts** | Bean Validation estrito em todos os DTOs (`@NotBlank`, `@Email`, `@Size`, `@DddValidation`, `@EnumValidation`). `GlobalExceptionHandler` captura `MethodArgumentNotValidException`, `ResourceNotFoundException`, `IllegalArgumentException` e `AuthenticationException` retornando respostas padronizadas. |
| **03** | **Segurança com JWT & RBAC** (múltiplos papéis de usuário) | **20 pts** | Spring Security 6 / OAuth2 Resource Server com tokens JWT assinados via par de chaves RSA 2048 bits geradas automaticamente pelo Gradle. Controle de acesso por papéis: `ROLE_CIDADAO` (munícipe: criar ocorrências, apoiar com votos, gerenciar próprio perfil), `ROLE_GESTOR` (agente da prefeitura: avaliar ocorrências e atualizar status de resolução dos chamados) e `ROLE_ADMIN` (administrador geral com CRUD completo de usuários, chamados, categorias, status, endereços e alteração de cargos). |
| **04** | **Spring AI Integrado ao Fluxo de Negócio** | **20 pts** | Integração multimodal via `ChatClient` com Groq (`qwen/qwen3.8-27b`) no `zelafix-worker`. O worker consome a mensagem da fila assíncrona com os bytes da foto, detecta o MIME type via `ImageMimeTypeDetector`, analisa visualmente a imagem e os dados do chamado, classifica a prioridade e o órgão municipal competente (`Structured Output` via `TriagemIaResult`) e publica o parecer técnico na fila `chamado.triagem-concluida.queue`. |
| **05** | **Arquitetura Distribuída Funcional (Opção A — RabbitMQ)** | **20 pts** | Implementação com 2 microsserviços autônomos (`zelafix-api` e `zelafix-worker` 100% Stateless). Mensageria bidirecional com RabbitMQ: DirectExchange `zelafix.direct`, filas duráveis (`chamado.triagem.queue`, `chamado.triagem-concluida.queue`, `chamado.voto.queue`) e agregação de votos com buffer em memória (`ConcurrentHashMap`) e flush periódico em lote (`@Scheduled`). |
| **06** | **Documentação OpenAPI/Swagger & Integração Mobile** | **10 pts** | Swagger interativo disponível em `http://localhost:8080/swagger-ui.html`. Integração 100% validada com o app React Native (`Mobile-Application-Development`), com rotas diretas padronizadas (`/chamados`, `/usuarios`, `/usuarios/me`, `/login`, etc.). |
| **TOTAL** | **Pontuação Total da Entrega** | **100 pts** | **Solução Completa, Testada e Validada.** |

---

## 🔑 3. Credenciais de Teste para Avaliação

O banco de dados é inicializado automaticamente com usuários de teste e dados pré-carregados via migrations versionadas do Flyway (`V1__criar_tabelas_zelafix.sql`, `V2__dados_iniciais_zelafix.sql` e `V3__admin_inicial.sql`):

| Papel | Nome | E-mail | Senha | Permissões |
| :--- | :--- | :--- | :--- | :--- |
| **Cidadão** | Carlos Munícipe | `cidadao@zelafix.com.br` | `Senha@123` | Criar chamados (iniciam como `PENDENTE`), apoiar ocorrências e gerenciar seu perfil. |
| **Funcionário da Prefeitura** | Mariana Servidora Pública | `gestor@zelafix.com.br` | `Senha@123` | Agente público da prefeitura: atualizar status de resolução dos chamados (`PENDENTE`, `EM_ANALISE`, `EM_EXECUCAO`, `CONCLUIDO`, `REJEITADO`). |
| **Administrador Geral** | Administrador ZelaFix | `admin@zelafix.com.br` | `123mudar` | Acesso administrativo irrestrito: CRUD completo de usuários, chamados, categorias, status, endereços e alteração de perfis/cargos. |

---

## 🚀 4. Guia de Execução Local

### Pré-requisitos
- **Java 25** (Amazon Corretto 25 configurado no sistema).
- **Docker & Docker Compose** (para subir o RabbitMQ).

### Passo 1: Subir o Broker de Mensageria (RabbitMQ)
Na raiz do diretório `Java-Advanced`, execute:
```bash
docker compose up -d
```
- O painel web de gestão do RabbitMQ estará acessível em: `http://localhost:15672` (login: `guest` / senha: `guest`).

### Passo 2: Executar o Microsserviço 1 (zelafix-api — Porta 8080)
Em um terminal:
```bash
# A partir da raiz de Java-Advanced
.\gradlew :zelafix-api:bootRun
```
- A API REST estará rodando em: `http://localhost:8080`
- Swagger UI interativo: `http://localhost:8080/swagger-ui.html`
- Banco de Dados Conectado: Oracle FIAP (`oracle.fiap.com.br:1521/orcl`)

### Passo 3: Executar o Microsserviço 2 (zelafix-worker — Porta 8081)
Em outro terminal:
```bash
# A partir da raiz de Java-Advanced
.\gradlew :zelafix-worker:bootRun
```
- O worker (100% Stateless, sem conexão a banco de dados) conectará automaticamente ao RabbitMQ para processar a triagem multimodal por IA (`qwen/qwen3.8-27b`) e a agregação de votos em memória, devolvendo os pareceres de triagem na fila `chamado.triagem-concluida.queue`.

---

## 🧪 5. Execução dos Testes Automatizados

Para rodar todos os testes unitários e de integração de ambos os microsserviços:
```bash
cd c:\Users\Enzo\new_backup\FIAP\_Projetos\Global_Solution_2\Java-Advanced
.\gradlew test
```
Ou individualmente:
```bash
.\gradlew :zelafix-api:test
.\gradlew :zelafix-worker:test
```

---

## 📱 6. Integração com o App Mobile (React Native Expo)

O app mobile em `Mobile-Application-Development` conecta-se diretamente à `zelafix-api`:
- **Android Emulator:** `http://10.0.2.2:8080`
- **iOS Simulator / Web:** `http://localhost:8080`

Todos os contratos de resposta foram alinhados para retornar o objeto atualizado do chamado em operações de apoio/voto (`POST /chamados/{id}/apoiar`), listas paginadas (`content[]`), autenticação JWT com persistência no SecureStore/AsyncStorage e visualização das fotos.

---

## 🤖 7. Declaração Obrigatória de Uso de IA Generativa

Em total conformidade com as diretrizes do regulamento da Global Solution 2026/2:
- **Ferramentas utilizadas:** Google Antigravity / Gemini 2.5 Flash / Claude 3.5 Sonnet no auxílio ao design arquitetural, estruturação do build Gradle multi-project e geração de testes unitários.
- **Spring AI:** O microsserviço `zelafix-worker` utiliza a biblioteca oficial `spring-ai-starter-model-openai` (Spring AI 2.0.1) consumindo modelos LLM para a triagem automatizada, classificação de urgência e parecer técnico em favor do cidadão e da gestão pública.
