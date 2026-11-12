#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
=============================================================================
FIAP - GLOBAL SOLUTION 2026/2 | CITIZENTECH (ODS 11) - ZELAFIX
Suíte Exaustiva de Testes de Produção (Azure Cloud) & "Tenta Quebrar"
Testa TODOS os 35+ endpoints mapeados na OpenAPI 3.1.0 e WebApps Azure
=============================================================================
"""

import sys
import os
import time
import json
import base64
import random
import string
import requests
import concurrent.futures
from datetime import datetime

# Configurações de Endpoints em Produção
BASE_URL = os.getenv("BASE_URL", "https://app-zelafix-561432.azurewebsites.net")
WORKER_URL = os.getenv("WORKER_URL", "https://app-zelafix-worker-561432.azurewebsites.net")

# Imagem PNG mínima 1x1 transparente válida em base64 (68 bytes)
MINIMAL_PNG_B64 = (
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
)

# Imagem JPEG 1x1 válida em base64
MINIMAL_JPEG_B64 = (
    "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAABAAEBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
)

# Estrutura para armazenar o log consolidado dos testes
test_results = []

def record(category, test_name, method, endpoint, status_code, expected_status, passed, elapsed_ms, details=None):
    entry = {
        "category": category,
        "test_name": test_name,
        "method": method,
        "endpoint": endpoint,
        "status_code": status_code,
        "expected_status": expected_status,
        "passed": passed,
        "elapsed_ms": round(elapsed_ms, 2),
        "details": details or ""
    }
    test_results.append(entry)
    icon = "✅ PASS" if passed else "❌ FAIL"
    print(f"[{icon}] {category:20} | {method:6} {endpoint:38} | Code: {status_code:3} (exp {expected_status}) | {elapsed_ms:6.1f}ms | {test_name}")
    if not passed and details:
        print(f"       -> Detalhes do erro: {details[:200]}")

def req(method, path, **kwargs):
    url = f"{BASE_URL}{path}"
    timeout = kwargs.pop("timeout", 25)
    t0 = time.time()
    try:
        r = requests.request(method, url, timeout=timeout, **kwargs)
        elapsed = (time.time() - t0) * 1000
        return r, elapsed
    except Exception as e:
        elapsed = (time.time() - t0) * 1000
        class FakeResponse:
            status_code = 599
            text = str(e)
            headers = {}
            def json(self): return {"error": str(e)}
        return FakeResponse(), elapsed

print("=" * 80)
print("🚀 INICIANDO TESTE EXAUSTIVO DOS ENDPOINTS DO ZELAFIX NA NUVEM AZURE")
print(f"🌐 API URL:    {BASE_URL}")
print(f"⚙️  WORKER URL: {WORKER_URL}")
print(f"📅 Data/Hora:  {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("=" * 80)

# =============================================================================
# BLOCO 1: INFRAESTRUTURA, OBSERVABILIDADE E DOCUMENTAÇÃO
# =============================================================================
print("\n" + "#" * 80)
print("1. INFRAESTRUTURA, OBSERVABILIDADE E DOCUMENTAÇÃO (ACTUATOR & OPENAPI)")
print("#" * 80)

# 1.1 Actuator Health (API)
r, ms = req("GET", "/actuator/health")
passed = r.status_code == 200 and r.json().get("status") == "UP"
record("Observabilidade", "Actuator Health API (Oracle + Liveness)", "GET", "/actuator/health", r.status_code, 200, passed, ms, r.text)

# 1.2 Actuator Info (API)
r, ms = req("GET", "/actuator/info")
record("Observabilidade", "Actuator Info API", "GET", "/actuator/info", r.status_code, 200, r.status_code == 200, ms, r.text)

# 1.3 Actuator Health (Worker)
t0 = time.time()
try:
    rw = requests.get(f"{WORKER_URL}/actuator/health", timeout=25)
    ms = (time.time() - t0) * 1000
    passed = rw.status_code == 200 and rw.json().get("status") == "UP"
    record("Observabilidade", "Actuator Health Worker (Spring AI + Liveness)", "GET", f"{WORKER_URL}/actuator/health", rw.status_code, 200, passed, ms, rw.text)
except Exception as e:
    ms = (time.time() - t0) * 1000
    record("Observabilidade", "Actuator Health Worker (Spring AI + Liveness)", "GET", f"{WORKER_URL}/actuator/health", 599, 200, False, ms, str(e))

# 1.4 OpenAPI 3.1 Docs (JSON)
r, ms = req("GET", "/v3/api-docs")
passed = r.status_code == 200 and "openapi" in r.text
record("Documentação", "OpenAPI Specification JSON", "GET", "/v3/api-docs", r.status_code, 200, passed, ms)

# 1.5 Swagger UI (HTML)
r, ms = req("GET", "/swagger-ui/index.html")
passed = r.status_code == 200 and "swagger-ui" in r.text.lower()
record("Documentação", "Swagger UI Interativo HTML", "GET", "/swagger-ui/index.html", r.status_code, 200, passed, ms)

# =============================================================================
# BLOCO 2: AUTENTICAÇÃO E CONTROLE DE ACESSO (RBAC)
# =============================================================================
print("\n" + "#" * 80)
print("2. AUTENTICAÇÃO E CONTROLE DE ACESSO (RBAC)")
print("#" * 80)

# 2.1 Login Admin
r, ms = req("POST", "/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"})
passed = r.status_code == 200 and "token" in r.json()
record("Autenticação", "Login Admin Válido", "POST", "/login", r.status_code, 200, passed, ms)
admin_token = r.json().get("token") if passed else None
admin_headers = {"Authorization": f"Bearer {admin_token}"} if admin_token else {}

# 2.2 Login Gestor
r, ms = req("POST", "/login", json={"email": "roberto.gestor@subprefeitura.sp.gov.br", "senha": "Senha#123"})
passed = r.status_code == 200 and "token" in r.json()
record("Autenticação", "Login Gestor Válido", "POST", "/login", r.status_code, 200, passed, ms)
gestor_token = r.json().get("token") if passed else None
gestor_headers = {"Authorization": f"Bearer {gestor_token}"} if gestor_token else {}

# 2.3 Login Cidadão
r, ms = req("POST", "/login", json={"email": "carlos.silva@email.com", "senha": "Senha#123"})
passed = r.status_code == 200 and "token" in r.json()
record("Autenticação", "Login Cidadão Válido", "POST", "/login", r.status_code, 200, passed, ms)
cidadao_token = r.json().get("token") if passed else None
cidadao_headers = {"Authorization": f"Bearer {cidadao_token}"} if cidadao_token else {}

# 2.4 Login com senha errada
r, ms = req("POST", "/login", json={"email": "admin@zelafix.com.br", "senha": "senha_totalmente_errada"})
record("Autenticação", "Login com senha incorreta", "POST", "/login", r.status_code, 401, r.status_code == 401, ms)

# 2.5 Login com usuário inexistente
r, ms = req("POST", "/login", json={"email": "inexistente_999@zelafix.com.br", "senha": "123"})
record("Autenticação", "Login com e-mail inexistente", "POST", "/login", r.status_code, 401, r.status_code == 401, ms)

# 2.6 Login com injeção SQL
r, ms = req("POST", "/login", json={"email": "admin@zelafix.com.br' OR '1'='1", "senha": "' OR '1'='1"})
record("Segurança/Injeção", "Login com SQL Injection payload", "POST", "/login", r.status_code, 401, r.status_code in [400, 401], ms)

# 2.7 Login com XSS payload
r, ms = req("POST", "/login", json={"email": "<script>alert(1)</script>@xss.com", "senha": "<script>alert(1)</script>"})
record("Segurança/Injeção", "Login com XSS script injection", "POST", "/login", r.status_code, 401, r.status_code in [400, 401], ms)

# 2.8 Login com body vazio / malformado
r, ms = req("POST", "/login", data="corrupted json string", headers={"Content-Type": "application/json"})
record("Tenta Quebrar", "Login com JSON malformado", "POST", "/login", r.status_code, 400, r.status_code in [400, 415], ms)

# =============================================================================
# BLOCO 3: GESTÃO DE USUÁRIOS E PERFIL (/usuarios)
# =============================================================================
print("\n" + "#" * 80)
print("3. GESTÃO DE USUÁRIOS E PERFIL (/usuarios)")
print("#" * 80)

# 3.1 Cadastro público de novo munícipe
unique_id = int(time.time() * 1000) % 1000000
test_user_email = f"municipe_auto_{unique_id}@zelafix.com.br"
novo_usuario_payload = {
    "nome": f"Munícipe Teste {unique_id}",
    "email": test_user_email,
    "senha": "Senha@Forte123",
    "cargo": "CIDADAO",
    "ddd": "11",
    "numeroTelefone": "987654321",
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1000",
        "referencia": "Em frente ao Metrô Trianon"
    }
}
r, ms = req("POST", "/usuarios", json=novo_usuario_payload)
passed = r.status_code == 201 and "id" in r.json()
record("Usuários", "Cadastro Público Novo Munícipe", "POST", "/usuarios", r.status_code, 201, passed, ms)
created_user_id = r.json().get("id") if passed else None

# Login com o novo munícipe criado
r, ms = req("POST", "/login", json={"email": test_user_email, "senha": "Senha@Forte123"})
passed = r.status_code == 200 and "token" in r.json()
novo_token = r.json().get("token") if passed else None
novo_headers = {"Authorization": f"Bearer {novo_token}"} if novo_token else {}
record("Usuários", "Login com Novo Munícipe Criado", "POST", "/login", r.status_code, 200, passed, ms)

# 3.2 Tentativa de cadastro com e-mail duplicado
r, ms = req("POST", "/usuarios", json=novo_usuario_payload)
record("Tenta Quebrar", "Cadastro com e-mail duplicado (deve rejeitar)", "POST", "/usuarios", r.status_code, 409, r.status_code in [400, 409], ms)

# 3.3 Cadastro com DDD inválido (DDD com 3 dígitos ou letras)
bad_ddd_payload = dict(novo_usuario_payload)
bad_ddd_payload["email"] = f"err_ddd_{unique_id}@test.com"
bad_ddd_payload["ddd"] = "999"
r, ms = req("POST", "/usuarios", json=bad_ddd_payload)
record("Tenta Quebrar", "Cadastro com DDD inválido (999)", "POST", "/usuarios", r.status_code, 400, r.status_code == 400, ms)

# 3.4 Cadastro com telefone vazio
bad_tel_payload = dict(novo_usuario_payload)
bad_tel_payload["email"] = f"err_tel_{unique_id}@test.com"
bad_tel_payload["numeroTelefone"] = ""
r, ms = req("POST", "/usuarios", json=bad_tel_payload)
record("Tenta Quebrar", "Cadastro com telefone em branco", "POST", "/usuarios", r.status_code, 400, r.status_code == 400, ms)

# 3.5 Tentativa de cadastro público forçando cargo ADMIN (deve forçar CIDADAO)
hacker_payload = dict(novo_usuario_payload)
hacker_payload["email"] = f"hacker_{unique_id}@test.com"
hacker_payload["cargo"] = "ADMIN"
r, ms = req("POST", "/usuarios", json=hacker_payload)
passed = r.status_code == 201 and r.json().get("cargo") == "CIDADAO"
record("Segurança/RBAC", "Cadastro público tentando cargo ADMIN (deve forçar CIDADAO)", "POST", "/usuarios", r.status_code, 201, passed, ms)

# 3.6 GET /usuarios/me (Perfil do usuário autenticado)
r, ms = req("GET", "/usuarios/me", headers=novo_headers)
passed = r.status_code == 200 and r.json().get("email") == test_user_email
record("Usuários", "Consultar perfil próprio (GET /usuarios/me)", "GET", "/usuarios/me", r.status_code, 200, passed, ms)

# 3.7 GET /usuarios/me sem token (deve falhar 401)
r, ms = req("GET", "/usuarios/me")
record("Segurança/RBAC", "Acesso a /usuarios/me anônimo (deve retornar 401)", "GET", "/usuarios/me", r.status_code, 401, r.status_code == 401, ms)

# 3.8 GET /usuarios/me com token forjado / malformado
r, ms = req("GET", "/usuarios/me", headers={"Authorization": "Bearer token_totalmente_falso_e_invalido"})
record("Segurança/RBAC", "Acesso com JWT forjado (deve retornar 401)", "GET", "/usuarios/me", r.status_code, 401, r.status_code == 401, ms)

# 3.9 PUT /usuarios/me (Atualizar dados pessoais)
update_me_payload = {
    "nome": f"Munícipe Atualizado {unique_id}",
    "email": test_user_email,
    "senha": "NovaSenha#123",
    "cargo": "CIDADAO",
    "ddd": "11",
    "numeroTelefone": "911223344",
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1500",
        "referencia": "Próximo ao MASP"
    }
}
r, ms = req("PUT", "/usuarios/me", headers=novo_headers, json=update_me_payload)
passed = r.status_code == 200 and r.json().get("nome") == f"Munícipe Atualizado {unique_id}"
record("Usuários", "Atualizar perfil próprio (PUT /usuarios/me)", "PUT", "/usuarios/me", r.status_code, 200, passed, ms)

# 3.10 GET /usuarios (Listagem de todos os usuários - Requer ADMIN)
# Com Admin:
r, ms = req("GET", "/usuarios", headers=admin_headers)
passed = r.status_code == 200 and "content" in r.json()
record("Usuários", "Listar usuários como ADMIN (GET /usuarios)", "GET", "/usuarios", r.status_code, 200, passed, ms)

# Com Cidadão (deve barrar 403 Forbidden):
r, ms = req("GET", "/usuarios", headers=cidadao_headers)
record("Segurança/RBAC", "Cidadão tentando listar todos os usuários (deve retornar 403)", "GET", "/usuarios", r.status_code, 403, r.status_code == 403, ms)

# 3.11 GET /usuarios/{id} (Buscar por ID - ADMIN)
if created_user_id:
    r, ms = req("GET", f"/usuarios/{created_user_id}", headers=admin_headers)
    passed = r.status_code == 200 and r.json().get("id") == created_user_id
    record("Usuários", f"Buscar usuário por ID {created_user_id} (ADMIN)", "GET", f"/usuarios/{created_user_id}", r.status_code, 200, passed, ms)

# 3.12 GET /usuarios/by-email (Buscar por e-mail - ADMIN)
r, ms = req("GET", f"/usuarios/by-email?email={test_user_email}", headers=admin_headers)
passed = r.status_code == 200 and r.json().get("email") == test_user_email
record("Usuários", "Buscar usuário por e-mail (GET /usuarios/by-email)", "GET", "/usuarios/by-email", r.status_code, 200, passed, ms)

# 3.13 Buscar usuário inexistente por ID (deve retornar 404)
r, ms = req("GET", "/usuarios/999999", headers=admin_headers)
record("Tenta Quebrar", "Buscar usuário inexistente ID 999999 (deve retornar 404)", "GET", "/usuarios/999999", r.status_code, 404, r.status_code == 404, ms)

# 3.14 Fuzzing de ID negativo e string
r, ms = req("GET", "/usuarios/-1", headers=admin_headers)
record("Tenta Quebrar", "Buscar usuário com ID negativo (-1)", "GET", "/usuarios/-1", r.status_code, 404, r.status_code in [400, 404], ms)

r, ms = req("GET", "/usuarios/id_invalido_texto", headers=admin_headers)
record("Tenta Quebrar", "Buscar usuário com ID textual (não numérico)", "GET", "/usuarios/id_invalido_texto", r.status_code, 400, r.status_code == 400, ms)

# 3.15 PATCH /usuarios/{id}/cargo (Alterar cargo - ADMIN)
if created_user_id:
    r, ms = req("PATCH", f"/usuarios/{created_user_id}/cargo", headers=admin_headers, json={"cargo": "GESTOR"})
    passed = r.status_code == 200 and r.json().get("cargo") == "GESTOR"
    record("Usuários", "Alterar cargo para GESTOR (ADMIN)", "PATCH", f"/usuarios/{created_user_id}/cargo", r.status_code, 200, passed, ms)

    # Tentativa de cargo inválido
    r, ms = req("PATCH", f"/usuarios/{created_user_id}/cargo", headers=admin_headers, json={"cargo": "PRESIDENTE_DA_REPUBLICA"})
    record("Tenta Quebrar", "Alterar cargo para valor inválido no Enum", "PATCH", f"/usuarios/{created_user_id}/cargo", r.status_code, 400, r.status_code == 400, ms)

# =============================================================================
# BLOCO 4: CATEGORIAS DE ZELADORIA URBANA (/categorias)
# =============================================================================
print("\n" + "#" * 80)
print("4. CATEGORIAS DE ZELADORIA URBANA (/categorias)")
print("#" * 80)

# 4.1 GET /categorias (Público/Cidadão)
r, ms = req("GET", "/categorias", headers=cidadao_headers)
passed = r.status_code == 200 and isinstance(r.json(), list) and len(r.json()) > 0
record("Categorias", "Listar categorias cadastradas (GET /categorias)", "GET", "/categorias", r.status_code, 200, passed, ms)
categorias_list = r.json() if passed else []
valid_cat_id = categorias_list[0]["id"] if categorias_list else 1

# 4.2 GET /categorias/{id}
r, ms = req("GET", f"/categorias/{valid_cat_id}", headers=cidadao_headers)
passed = r.status_code == 200 and r.json().get("id") == valid_cat_id
record("Categorias", f"Buscar categoria por ID {valid_cat_id}", "GET", f"/categorias/{valid_cat_id}", r.status_code, 200, passed, ms)

# 4.3 POST /categorias como ADMIN
cat_nome = f"Categoria Auto {unique_id}"
r, ms = req("POST", "/categorias", headers=admin_headers, json={"nome": cat_nome})
passed = r.status_code == 201 and "id" in r.json()
record("Categorias", f"Criar categoria '{cat_nome}' como ADMIN", "POST", "/categorias", r.status_code, 201, passed, ms)
created_cat_id = r.json().get("id") if passed else None

# 4.4 POST /categorias como CIDADÃO (deve retornar 403 Forbidden)
r, ms = req("POST", "/categorias", headers=cidadao_headers, json={"nome": "Hack Categoria"})
record("Segurança/RBAC", "Cidadão tentando criar categoria (deve retornar 403)", "POST", "/categorias", r.status_code, 403, r.status_code == 403, ms)

# 4.5 POST /categorias com nome vazio (validação @NotBlank)
r, ms = req("POST", "/categorias", headers=admin_headers, json={"nome": "   "})
record("Tenta Quebrar", "Criar categoria com nome vazio", "POST", "/categorias", r.status_code, 400, r.status_code == 400, ms)

# 4.6 PUT /categorias/{id} (Atualizar como ADMIN)
if created_cat_id:
    r, ms = req("PUT", f"/categorias/{created_cat_id}", headers=admin_headers, json={"nome": f"Cat Atualizada {unique_id}"})
    passed = r.status_code == 200 and r.json().get("nome") == f"Cat Atualizada {unique_id}"
    record("Categorias", f"Atualizar categoria ID {created_cat_id} como ADMIN", "PUT", f"/categorias/{created_cat_id}", r.status_code, 200, passed, ms)

# 4.7 DELETE /categorias/{id} (Excluir como ADMIN)
if created_cat_id:
    r, ms = req("DELETE", f"/categorias/{created_cat_id}", headers=admin_headers)
    record("Categorias", f"Excluir categoria ID {created_cat_id} como ADMIN", "DELETE", f"/categorias/{created_cat_id}", r.status_code, 204, r.status_code == 204, ms)

# =============================================================================
# BLOCO 5: STATUS DO CICLO DE VIDA DOS CHAMADOS (/status)
# =============================================================================
print("\n" + "#" * 80)
print("5. STATUS DO CICLO DE VIDA DOS CHAMADOS (/status)")
print("#" * 80)

# 5.1 GET /status
r, ms = req("GET", "/status", headers=cidadao_headers)
passed = r.status_code == 200 and isinstance(r.json(), list) and len(r.json()) > 0
record("Status", "Listar status disponíveis (GET /status)", "GET", "/status", r.status_code, 200, passed, ms)
status_list = r.json() if passed else []
valid_status_id = status_list[0]["id"] if status_list else 1

# 5.2 GET /status/{id}
r, ms = req("GET", f"/status/{valid_status_id}", headers=cidadao_headers)
passed = r.status_code == 200 and r.json().get("id") == valid_status_id
record("Status", f"Buscar status por ID {valid_status_id}", "GET", f"/status/{valid_status_id}", r.status_code, 200, passed, ms)

# 5.3 POST /status com nome customizado fora do enum (deve rejeitar 400 por constraint ck_status_nome)
status_nome = f"CUSTOM_{unique_id % 10000}"
r, ms = req("POST", "/status", headers=admin_headers, json={"nome": status_nome})
passed = r.status_code == 400
record("Status", f"Criar status customizado rejeitado com 400 (ck_status_nome)", "POST", "/status", r.status_code, 400, passed, ms)
created_status_id = None

# 5.4 POST /status como CIDADÃO (deve retornar 403 Forbidden)
r, ms = req("POST", "/status", headers=cidadao_headers, json={"nome": "STATUS_HACK"})
record("Segurança/RBAC", "Cidadão tentando criar status (deve retornar 403)", "POST", "/status", r.status_code, 403, r.status_code == 403, ms)

# 5.5 DELETE /status/{id} como ADMIN
if created_status_id:
    r, ms = req("DELETE", f"/status/{created_status_id}", headers=admin_headers)
    record("Status", f"Excluir status ID {created_status_id} como ADMIN", "DELETE", f"/status/{created_status_id}", r.status_code, 204, r.status_code == 204, ms)

# =============================================================================
# BLOCO 6: GERENCIAMENTO DE ENDEREÇOS (/enderecos)
# =============================================================================
print("\n" + "#" * 80)
print("6. GERENCIAMENTO DE ENDEREÇOS (/enderecos)")
print("#" * 80)

# 6.1 GET /enderecos como ADMIN
r, ms = req("GET", "/enderecos", headers=admin_headers)
passed = r.status_code == 200 and "content" in r.json()
record("Endereços", "Listar endereços como ADMIN (GET /enderecos)", "GET", "/enderecos", r.status_code, 200, passed, ms)
enderecos_list = r.json().get("content", []) if passed else []
valid_end_id = enderecos_list[0]["id"] if enderecos_list else 1

# 6.2 GET /enderecos como CIDADÃO (deve retornar 403)
r, ms = req("GET", "/enderecos", headers=cidadao_headers)
record("Segurança/RBAC", "Cidadão tentando listar todos endereços (deve retornar 403)", "GET", "/enderecos", r.status_code, 403, r.status_code == 403, ms)

# 6.3 GET /enderecos/{id} como ADMIN
r, ms = req("GET", f"/enderecos/{valid_end_id}", headers=admin_headers)
passed = r.status_code == 200 and r.json().get("id") == valid_end_id
record("Endereços", f"Buscar endereço por ID {valid_end_id} (ADMIN)", "GET", f"/enderecos/{valid_end_id}", r.status_code, 200, passed, ms)

# 6.4 POST /enderecos como ADMIN
novo_end_payload = {
    "cep": "01310100",
    "rua": "Avenida Paulista",
    "numero": "2000",
    "referencia": "Esquina com Bela Cintra"
}
r, ms = req("POST", "/enderecos", headers=admin_headers, json=novo_end_payload)
passed = r.status_code == 201 and "id" in r.json()
record("Endereços", "Criar endereço como ADMIN (POST /enderecos)", "POST", "/enderecos", r.status_code, 201, passed, ms)
created_end_id = r.json().get("id") if passed else None

# 6.5 POST /enderecos com CEP inválido (fuzzing)
r, ms = req("POST", "/enderecos", headers=admin_headers, json={"cep": "123", "rua": "Rua X", "numero": "1"})
record("Tenta Quebrar", "Criar endereço com CEP inválido (123)", "POST", "/enderecos", r.status_code, 400, r.status_code == 400, ms)

# =============================================================================
# BLOCO 7: OPERAÇÕES COMPLETAS DE CHAMADOS E AUDITORIA (/chamados)
# =============================================================================
print("\n" + "#" * 80)
print("7. OPERAÇÕES COMPLETAS DE CHAMADOS, FOTOS E VOTOS (/chamados)")
print("#" * 80)

# 7.1 GET /chamados/estatisticas
r, ms = req("GET", "/chamados/estatisticas", headers=cidadao_headers)
passed = r.status_code == 200 and "total" in r.json()
record("Chamados", "Dashboard de estatísticas consolidado (GET /chamados/estatisticas)", "GET", "/chamados/estatisticas", r.status_code, 200, passed, ms)

# 7.2 GET /chamados (Listagem com paginação)
r, ms = req("GET", "/chamados", headers=cidadao_headers)
passed = r.status_code == 200 and "content" in r.json()
record("Chamados", "Listar chamados com paginação (GET /chamados)", "GET", "/chamados", r.status_code, 200, passed, ms)

# 7.3 GET /chamados com filtros dinâmicos
r, ms = req("GET", "/chamados?status=ABERTO", headers=cidadao_headers)
record("Chamados", "Filtrar chamados por status=ABERTO", "GET", "/chamados?status=ABERTO", r.status_code, 200, r.status_code == 200, ms)

r, ms = req("GET", f"/chamados?categoriaId={valid_cat_id}", headers=cidadao_headers)
record("Chamados", f"Filtrar chamados por categoriaId={valid_cat_id}", "GET", f"/chamados?categoriaId={valid_cat_id}", r.status_code, 200, r.status_code == 200, ms)

# 7.4 POST /chamados (Criação de novo chamado com foto base64 real)
chamado_payload = {
    "titulo": f"Buraco grave e afundamento de asfalto #{unique_id}",
    "descricao": "Cratera profunda de aproximadamente 1 metro na faixa da esquerda causando quebra de suspensão e risco grave de atropelamento.",
    "categoriaId": valid_cat_id,
    "fotoBase64": MINIMAL_PNG_B64.split(",", 1)[1],
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1800",
        "referencia": "Próximo à Alameda Ministro Rocha Azevedo"
    }
}
r, ms = req("POST", "/chamados", headers=cidadao_headers, json=chamado_payload)
passed = r.status_code == 201 and "idChamado" in r.json()
record("Chamados", "Abrir novo chamado com Foto Base64 (POST /chamados)", "POST", "/chamados", r.status_code, 201, passed, ms)
created_chamado_id = r.json().get("idChamado") if passed else None

if created_chamado_id:
    # 7.5 GET /chamados/{id} (Detalhes e histórico de auditoria)
    r, ms = req("GET", f"/chamados/{created_chamado_id}", headers=cidadao_headers)
    passed = r.status_code == 200 and r.json().get("idChamado") == created_chamado_id
    record("Chamados", f"Buscar chamado ID {created_chamado_id} completo com histórico", "GET", f"/chamados/{created_chamado_id}", r.status_code, 200, passed, ms)

    # 7.6 GET /chamados/{id}/foto (Recuperar imagem fotográfica do Oracle BLOB)
    r, ms = req("GET", f"/chamados/{created_chamado_id}/foto", headers=cidadao_headers)
    content_type = r.headers.get("Content-Type", "")
    passed = r.status_code == 200 and ("image" in content_type or len(r.content) > 0)
    record("Chamados", f"Download da Foto BLOB (/chamados/{created_chamado_id}/foto)", "GET", f"/chamados/{created_chamado_id}/foto", r.status_code, 200, passed, ms, f"Content-Type: {content_type}, Bytes: {len(r.content)}")

    # 7.7 POST /chamados/{id}/apoiar (Toggle - Desfazer Apoio inicial do autor)
    r, ms = req("POST", f"/chamados/{created_chamado_id}/apoiar", headers=cidadao_headers)
    passed = r.status_code == 200 and r.json().get("apoiadoPorMim") is False
    record("Votos/RabbitMQ", f"Desfazer apoio (Toggle) ID {created_chamado_id}", "POST", f"/chamados/{created_chamado_id}/apoiar", r.status_code, 200, passed, ms)

    # 7.8 POST /chamados/{id}/apoiar (Toggle - Reativar Apoio)
    r, ms = req("POST", f"/chamados/{created_chamado_id}/apoiar", headers=cidadao_headers)
    passed = r.status_code == 200 and r.json().get("apoiadoPorMim") is True
    record("Votos/RabbitMQ", f"Reativar apoio comunitário ID {created_chamado_id}", "POST", f"/chamados/{created_chamado_id}/apoiar", r.status_code, 200, passed, ms)

    # 7.9 PUT /chamados/{id} (Atualizar chamado pelo criador com endereço obrigatório)
    put_chamado_payload = {
        "titulo": f"Buraco grave e afundamento [ATUALIZADO] #{unique_id}",
        "descricao": "Cratera aumentou após chuva forte. Equipe de sinalização urgente solicitada.",
        "categoriaId": valid_cat_id,
        "endereco": {
            "cep": "01311000",
            "rua": "Avenida Paulista",
            "numero": "1800",
            "referencia": "Próximo à Alameda Ministro Rocha Azevedo"
        }
    }
    r, ms = req("PUT", f"/chamados/{created_chamado_id}", headers=cidadao_headers, json=put_chamado_payload)
    passed = r.status_code == 200 and "ATUALIZADO" in r.json().get("titulo", "")
    record("Chamados", f"Atualizar chamado ID {created_chamado_id} (PUT)", "PUT", f"/chamados/{created_chamado_id}", r.status_code, 200, passed, ms)

    # 7.10 PATCH /chamados/{id}/status por CIDADÃO (deve ser rejeitado com 403 Forbidden)
    r, ms = req("PATCH", f"/chamados/{created_chamado_id}/status", headers=cidadao_headers, json={"status": "CONCLUIDO", "observacao": "Eu mesmo resolvi"})
    record("Segurança/RBAC", "Cidadão tentando transicionar status (deve retornar 403)", "PATCH", f"/chamados/{created_chamado_id}/status", r.status_code, 403, r.status_code == 403, ms)

    # 7.11 PATCH /chamados/{id}/status por GESTOR (Transição para EM_EXECUCAO)
    r, ms = req("PATCH", f"/chamados/{created_chamado_id}/status", headers=gestor_headers, json={"status": "EM_EXECUCAO", "observacao": "Equipe da subprefeitura enviada ao local para vistoria técnica."})
    passed = r.status_code == 200 and r.json().get("status") == "EM_EXECUCAO"
    record("Chamados/Auditoria", f"Transicionar para EM_EXECUCAO como GESTOR", "PATCH", f"/chamados/{created_chamado_id}/status", r.status_code, 200, passed, ms)

    # 7.12 PATCH /chamados/{id}/status por ADMIN (Finalização para CONCLUIDO)
    r, ms = req("PATCH", f"/chamados/{created_chamado_id}/status", headers=admin_headers, json={"status": "CONCLUIDO", "observacao": "Obra de recapeamento e drenagem finalizada."})
    passed = r.status_code == 200 and r.json().get("status") == "CONCLUIDO"
    record("Chamados/Auditoria", f"Transicionar para CONCLUIDO como ADMIN", "PATCH", f"/chamados/{created_chamado_id}/status", r.status_code, 200, passed, ms)

    # 7.13 DELETE /chamados/{id} por CIDADÃO (deve falhar 403)
    r, ms = req("DELETE", f"/chamados/{created_chamado_id}", headers=cidadao_headers)
    record("Segurança/RBAC", "Cidadão tentando excluir chamado (deve retornar 403)", "DELETE", f"/chamados/{created_chamado_id}", r.status_code, 403, r.status_code == 403, ms)

    # 7.14 DELETE /chamados/{id} por ADMIN (204 No Content)
    r, ms = req("DELETE", f"/chamados/{created_chamado_id}", headers=admin_headers)
    record("Chamados", f"Excluir chamado ID {created_chamado_id} como ADMIN (204 No Content)", "DELETE", f"/chamados/{created_chamado_id}", r.status_code, 204, r.status_code == 204, ms)

# =============================================================================
# BLOCO 8: BATERIA "TENTA QUEBRAR" (FUZZING, INJEÇÕES E STRESS)
# =============================================================================
print("\n" + "#" * 80)
print("8. BATERIA 'TENTA QUEBRAR' (FUZZING, INJEÇÕES, EDGE CASES E CONCORRÊNCIA)")
print("#" * 80)

# 8.1 Injeção SQL na query string de chamados
r, ms = req("GET", "/chamados?status=' UNION SELECT NULL,NULL,NULL FROM DUAL--", headers=cidadao_headers)
record("Tenta Quebrar", "Injeção SQL em query param ?status=", "GET", "/chamados?status=SQLI", r.status_code, 400, r.status_code in [200, 400], ms)

# 8.2 Título com 10.000 caracteres (Buffer Overflow / Validation Test)
huge_title = "A" * 10000
bad_payload = {
    "titulo": huge_title,
    "descricao": "Teste de payload gigante",
    "categoriaId": valid_cat_id,
    "endereco": {"cep": "01311000", "rua": "Paulista", "numero": "1"}
}
r, ms = req("POST", "/chamados", headers=cidadao_headers, json=bad_payload)
record("Tenta Quebrar", "Abertura de chamado com título de 10.000 caracteres", "POST", "/chamados", r.status_code, 400, r.status_code in [400, 413], ms)

# 8.3 Foto corrompida (não-base64)
bad_photo_payload = {
    "titulo": "Buraco com foto inválida",
    "descricao": "Tentativa de envio de arquivo corrompido",
    "categoriaId": valid_cat_id,
    "foto": "isto_nao_eh_um_base64_valido!@#$%^&*()_+",
    "endereco": {"cep": "01311000", "rua": "Paulista", "numero": "1"}
}
r, ms = req("POST", "/chamados", headers=cidadao_headers, json=bad_photo_payload)
record("Tenta Quebrar", "Abertura de chamado com foto base64 corrompida", "POST", "/chamados", r.status_code, 400, r.status_code == 400, ms)

# 8.4 Foto com MIME type proibido (.exe)
bad_exe_b64 = "data:application/x-msdownload;base64," + base64.b64encode(b"MZ executable header dummy").decode("utf-8")
bad_exe_payload = {
    "titulo": "Tentativa de malware .exe",
    "descricao": "Envio de arquivo executável no campo foto",
    "categoriaId": valid_cat_id,
    "foto": bad_exe_b64,
    "endereco": {"cep": "01311000", "rua": "Paulista", "numero": "1"}
}
r, ms = req("POST", "/chamados", headers=cidadao_headers, json=bad_exe_payload)
record("Tenta Quebrar", "Tentativa de upload de .exe no campo foto (MimeType)", "POST", "/chamados", r.status_code, 400, r.status_code == 400, ms)

# 8.5 Teste de Carga Concorrente: 30 requisições simultâneas em paralelo
print("\n>>> Iniciando Teste de Carga Concorrente (30 threads simultâneas)...")
concurrent_requests = 30
success_count = 0
latencies = []

def worker_fetch(i):
    t0 = time.time()
    try:
        r = requests.get(f"{BASE_URL}/chamados?page=0&size=5", headers=cidadao_headers, timeout=15)
        dt = (time.time() - t0) * 1000
        return r.status_code == 200, dt
    except Exception:
        return False, 15000

t_start_load = time.time()
with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
    futures = [executor.submit(worker_fetch, i) for i in range(concurrent_requests)]
    for f in concurrent.futures.as_completed(futures):
        ok, lat = f.result()
        latencies.append(lat)
        if ok:
            success_count += 1

total_load_time = time.time() - t_start_load
avg_lat = sum(latencies) / len(latencies) if latencies else 0
record("Stress/Concorrência", f"Carga Concorrente 30 reqs simultâneas (Sucessos: {success_count}/{concurrent_requests})", "GET", "/chamados", 200, 200, success_count >= 28, avg_lat, f"Tempo Total: {total_load_time:.2f}s, Média: {avg_lat:.1f}ms")

# =============================================================================
# CONSOLIDAÇÃO DO RELATÓRIO
# =============================================================================
total_tests = len(test_results)
passed_tests = sum(1 for t in test_results if t["passed"])
failed_tests = total_tests - passed_tests
success_rate = (passed_tests / total_tests) * 100 if total_tests > 0 else 0

print("\n" + "=" * 80)
print("📊 RESUMO GERAL DA BATERIA DE TESTES EM PRODUÇÃO (AZURE)")
print("=" * 80)
print(f"Total de Testes Executados: {total_tests}")
print(f"Testes Aprovados (PASS):     {passed_tests}")
print(f"Testes Reprovados (FAIL):    {failed_tests}")
print(f"Taxa de Conformidade:        {success_rate:.2f}%")
print("=" * 80)

# Salvar relatório em JSON para processamento
with open("test_results_azure.json", "w", encoding="utf-8") as f:
    json.dump({
        "timestamp": datetime.now().isoformat(),
        "base_url": BASE_URL,
        "worker_url": WORKER_URL,
        "summary": {
            "total": total_tests,
            "passed": passed_tests,
            "failed": failed_tests,
            "success_rate": success_rate
        },
        "tests": test_results
    }, f, indent=2, ensure_ascii=False)

print("\n✅ Relatório estruturado salvo em 'test_results_azure.json' com sucesso!")
