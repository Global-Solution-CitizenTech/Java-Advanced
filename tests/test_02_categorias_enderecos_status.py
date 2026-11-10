import requests
import random

BASE_URL = "http://localhost:8080"

def test_categorias_enderecos_status():
    print("=" * 60)
    print("SUITE 2: CATEGORIAS, STATUS E GERENCIAMENTO DE ENDEREÇOS")
    print("=" * 60)

    # Obter token de Admin e Cidadão
    admin_auth = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"}).json()
    admin_headers = {"Authorization": f"Bearer {admin_auth['token']}"}

    cidadao_auth = requests.post(f"{BASE_URL}/login", json={"email": "carlos.silva@email.com", "senha": "Senha#123"}).json()
    cidadao_headers = {"Authorization": f"Bearer {cidadao_auth['token']}"}

    # 2.1 Listar categorias
    print("\n[2.1] Listando categorias cadastradas (GET /categorias)...")
    res = requests.get(f"{BASE_URL}/categorias", headers=cidadao_headers)
    assert res.status_code == 200, f"Falha ao listar categorias: {res.status_code} - {res.text}"
    categorias = res.json()
    assert len(categorias) > 0, "Deveria haver categorias cadastradas"
    print(f" -> Sucesso! Encontradas {len(categorias)} categorias:")
    for cat in categorias[:3]:
        print(f"    - ID {cat['id']}: {cat['nome']}")

    # 2.2 Criar categoria como Admin
    nova_cat_nome = f"Zeladoria Especial {random.randint(1000, 9999)}"
    print(f"\n[2.2] Criando nova categoria '{nova_cat_nome}' como Admin (POST /categorias)...")
    res = requests.post(f"{BASE_URL}/categorias", headers=admin_headers, json={"nome": nova_cat_nome})
    assert res.status_code == 201, f"Falha ao criar categoria: {res.status_code} - {res.text}"
    cat_criada = res.json()
    print(f" -> Sucesso! Categoria criada com ID {cat_criada['id']} - Nome: {cat_criada['nome']}")

    # 2.3 Tentativa de criar categoria com nome vazio (Validação de Bean)
    print("\n[2.3] Tentando criar categoria com nome vazio...")
    res = requests.post(f"{BASE_URL}/categorias", headers=admin_headers, json={"nome": "   "})
    assert res.status_code == 400, f"Deveria retornar 400, retornou {res.status_code}"
    print(f" -> Sucesso! Bloqueado com 400 Bad Request.")

    # 2.4 Listar Status disponíveis
    print("\n[2.4] Listando status do sistema (GET /status)...")
    res = requests.get(f"{BASE_URL}/status", headers=cidadao_headers)
    assert res.status_code == 200, f"Falha ao listar status: {res.status_code} - {res.text}"
    statuses = res.json()
    status_nomes = [s["nome"] for s in statuses]
    print(f" -> Sucesso! Status disponíveis: {status_nomes}")
    assert "PENDENTE" in status_nomes, "Status PENDENTE ausente"

    # 2.5 Admin listando endereços (GET /enderecos)
    print("\n[2.5] Admin listando endereços cadastrados (GET /enderecos)...")
    res = requests.get(f"{BASE_URL}/enderecos", headers=admin_headers)
    assert res.status_code == 200, f"Falha ao listar endereços: {res.status_code} - {res.text}"
    page = res.json()
    assert "content" in page and len(page["content"]) > 0, "Deveria haver endereços cadastrados"
    total = page['page']['totalElements'] if 'page' in page else len(page['content'])
    print(f" -> Sucesso! {total} endereços no banco. Primeiro: {page['content'][0]['rua']}, {page['content'][0]['bairro']}")

    # 2.6 Cidadão tentando acessar GET /enderecos (deve dar 403 Forbidden)
    print("\n[2.6] Cidadão comum tentando listar /enderecos (deve ser 403)...")
    res = requests.get(f"{BASE_URL}/enderecos", headers=cidadao_headers)
    assert res.status_code == 403, f"Deveria retornar 403, retornou {res.status_code}"
    print(" -> Sucesso! Bloqueado com 403 Forbidden.")

    # 2.7 Admin criando endereço com ViaCep integrado (POST /enderecos)
    print("\n[2.7] Admin criando endereço (POST /enderecos)...")
    novo_end_payload = {
        "cep": "01310-100",
        "numero": "2500",
        "referencia": "Edifício Comercial",
        "rua": "Avenida Paulista"
    }
    res = requests.post(f"{BASE_URL}/enderecos", headers=admin_headers, json=novo_end_payload)
    assert res.status_code == 201, f"Falha ao criar endereço: {res.status_code} - {res.text}"
    end_criado = res.json()
    assert end_criado["cep"] == "01310100", f"CEP incorreto: {end_criado['cep']}"
    print(f" -> Sucesso! Endereço criado com ID {end_criado['id']} via ViaCep: {end_criado['rua']}, {end_criado['bairro']}")

    # 2.8 Buscar endereço por ID (GET /enderecos/{id})
    print(f"\n[2.8] Buscando endereço por ID {end_criado['id']}...")
    res = requests.get(f"{BASE_URL}/enderecos/{end_criado['id']}", headers=admin_headers)
    assert res.status_code == 200, f"Falha: {res.status_code} - {res.text}"
    print(" -> Sucesso! Endereço recuperado com sucesso.")

    # 2.9 Tentativa de criar endereço com CEP inválido (formato errado)
    print("\n[2.9] Tentando criar endereço com CEP em formato inválido 'ABC-XYZ'...")
    res = requests.post(
        f"{BASE_URL}/enderecos",
        headers=admin_headers,
        json={"cep": "ABC-XYZ", "numero": "100", "referencia": "Esquina"}
    )
    assert res.status_code == 400, f"Deveria retornar 400, retornou {res.status_code}"
    print(" -> Sucesso! Validação de formato de CEP retornou 400 Bad Request.")

    print("\n" + "=" * 60)
    print("SUITE 2: TODOS OS 9 TESTES DE CATEGORIAS, STATUS E ENDEREÇO PASSARAM!")
    print("=" * 60)

if __name__ == "__main__":
    test_categorias_enderecos_status()
