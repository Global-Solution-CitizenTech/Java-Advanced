import requests
import json

BASE_URL = "http://localhost:8080"

def test_auth_and_security():
    print("=" * 60)
    print("SUITE 1: AUTENTICAÇÃO, REGISTRO E CONTROLE DE ACESSO (RBAC)")
    print("=" * 60)

    # 1.1 Login com credenciais válidas (Admin)
    print("\n[1.1] Testando login válido de Administrador (admin@zelafix.com.br)...")
    res = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"})
    assert res.status_code == 200, f"Falha no login do admin: {res.status_code} - {res.text}"
    admin_token = res.json()["token"]
    admin_user = res.json()["user"]
    assert admin_user["cargo"] == "ADMIN", f"Cargo incorreto: {admin_user['cargo']}"
    print(f" -> Sucesso! Admin ID={admin_user['id']}, Cargo={admin_user['cargo']}")

    # 1.2 Login de Gestor
    print("\n[1.2] Testando login válido de Gestor (roberto.gestor@subprefeitura.sp.gov.br)...")
    res = requests.post(f"{BASE_URL}/login", json={"email": "roberto.gestor@subprefeitura.sp.gov.br", "senha": "Senha#123"})
    assert res.status_code == 200, f"Falha no login do gestor: {res.status_code} - {res.text}"
    gestor_token = res.json()["token"]
    gestor_user = res.json()["user"]
    assert gestor_user["cargo"] == "GESTOR", f"Cargo incorreto: {gestor_user['cargo']}"
    print(f" -> Sucesso! Gestor ID={gestor_user['id']}, Cargo={gestor_user['cargo']}")

    # 1.3 Login de Cidadão
    print("\n[1.3] Testando login válido de Cidadão (carlos.silva@email.com)...")
    res = requests.post(f"{BASE_URL}/login", json={"email": "carlos.silva@email.com", "senha": "Senha#123"})
    assert res.status_code == 200, f"Falha no login do cidadão: {res.status_code} - {res.text}"
    cidadao_token = res.json()["token"]
    cidadao_user = res.json()["user"]
    assert cidadao_user["cargo"] == "CIDADAO", f"Cargo incorreto: {cidadao_user['cargo']}"
    print(f" -> Sucesso! Cidadão ID={cidadao_user['id']}, Cargo={cidadao_user['cargo']}")

    # 1.4 Login com senha incorreta
    print("\n[1.4] Testando login com senha errada (deve retornar 401)...")
    res = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "senha_totalmente_errada"})
    assert res.status_code == 401, f"Deveria retornar 401, retornou {res.status_code}"
    print(f" -> Sucesso! Retornou 401 Unauthorized corretamente.")

    # 1.5 Login com usuário inexistente
    print("\n[1.5] Testando login com e-mail inexistente...")
    res = requests.post(f"{BASE_URL}/login", json={"email": "fantasma@inexistente.com.br", "senha": "123"})
    assert res.status_code == 401, f"Deveria retornar 401, retornou {res.status_code}"
    print(f" -> Sucesso! Retornou 401.")

    # 1.6 Injeção SQL no login
    print("\n[1.6] Testando tentativa de SQL Injection no Login...")
    sql_injection = "' OR '1'='1' --"
    res = requests.post(f"{BASE_URL}/login", json={"email": sql_injection, "senha": "qualquer_coisa"})
    assert res.status_code in [400, 401], f"Deveria falhar com 400 ou 401, retornou {res.status_code}"
    print(f" -> Sucesso! Bloqueado com status {res.status_code}.")

    # 1.7 Acesso sem token em endpoint protegido
    print("\n[1.7] Testando acesso a /chamados sem cabeçalho Authorization...")
    res = requests.get(f"{BASE_URL}/chamados")
    assert res.status_code == 401, f"Deveria retornar 401, retornou {res.status_code}"
    print(f" -> Sucesso! Bloqueado com 401 Unauthorized.")

    # 1.8 Acesso com token JWT corrompido
    print("\n[1.8] Testando token JWT forjado/corrompido...")
    bad_headers = {"Authorization": "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.e30.invalidsignature"}
    res = requests.get(f"{BASE_URL}/chamados", headers=bad_headers)
    assert res.status_code == 401, f"Deveria retornar 401, retornou {res.status_code}"
    print(f" -> Sucesso! Token forjado rejeitado com 401.")

    # 1.9 Permissão RBAC: Cidadão tentando criar categoria (deve retornar 403 Forbidden)
    print("\n[1.9] Cidadão tentando POST /categorias (restrito a GESTOR/ADMIN)...")
    res = requests.post(
        f"{BASE_URL}/categorias",
        headers={"Authorization": f"Bearer {cidadao_token}"},
        json={"nome": "Categoria Proibida"}
    )
    assert res.status_code == 403, f"Deveria retornar 403 Forbidden, retornou {res.status_code}: {res.text}"
    print(f" -> Sucesso! Acesso negado com 403 Forbidden.")

    # 1.10 Permissão RBAC: Cidadão tentando alterar status de chamado (deve dar 403)
    print("\n[1.10] Cidadão tentando PATCH /chamados/1/status...")
    res = requests.patch(
        f"{BASE_URL}/chamados/1/status",
        headers={"Authorization": f"Bearer {cidadao_token}"},
        json={"status": "CONCLUIDO", "observacao": "Eu mesmo resolvi"}
    )
    assert res.status_code == 403, f"Deveria retornar 403, retornou {res.status_code}: {res.text}"
    print(f" -> Sucesso! Tentativa barrada com 403 Forbidden.")

    # 1.11 Registro de novo Cidadão via API
    print("\n[1.11] Testando registro de novo usuário Cidadão...")
    import random
    unique_num = random.randint(10000, 99999)
    novo_email = f"cidadao_teste_{unique_num}@email.com"
    novo_usuario_payload = {
        "nome": f"Cidadão Teste {unique_num}",
        "email": novo_email,
        "senha": "SenhaForte@123",
        "cargo": "CIDADAO",
        "ddd": "11",
        "numeroTelefone": f"9{unique_num:08d}",
        "endereco": {
            "cep": "01310-100",
            "numero": "2000",
            "referencia": "Apto 42",
            "rua": "Avenida Paulista"
        }
    }
    res = requests.post(f"{BASE_URL}/usuarios", json=novo_usuario_payload)
    assert res.status_code == 201, f"Falha ao registrar novo cidadão: {res.status_code} - {res.text}"
    novo_usuario_id = res.json()["id"]
    print(f" -> Sucesso! Novo cidadão cadastrado com ID {novo_usuario_id} e email {novo_email}")

    # 1.12 Tentativa de registro de e-mail duplicado
    print("\n[1.12] Tentando registrar o mesmo e-mail novamente (deve falhar)...")
    res = requests.post(f"{BASE_URL}/usuarios", json=novo_usuario_payload)
    assert res.status_code in [400, 409], f"Deveria retornar 400 ou 409, retornou {res.status_code}: {res.text}"
    print(f" -> Sucesso! Rejeição de e-mail duplicado com status {res.status_code}.")

    # 1.13 Login com o novo cidadão criado
    print("\n[1.13] Testando login imediato do novo cidadão recém-cadastrado...")
    res = requests.post(f"{BASE_URL}/login", json={"email": novo_email, "senha": "SenhaForte@123"})
    assert res.status_code == 200, f"Falha no login do novo cidadão: {res.status_code}"
    print(" -> Sucesso! Token emitido para a nova conta.")

    print("\n" + "=" * 60)
    print("SUITE 1: TODOS OS 13 TESTES DE AUTENTICAÇÃO E RBAC PASSARAM!")
    print("=" * 60)

if __name__ == "__main__":
    test_auth_and_security()
