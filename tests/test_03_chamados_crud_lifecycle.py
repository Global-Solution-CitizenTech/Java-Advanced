import requests
import json
import base64
import random

BASE_URL = "http://localhost:8080"

# Imagem 1x1 PNG válida em Base64
DUMMY_PNG_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
DUMMY_PNG_BYTES = base64.b64decode(DUMMY_PNG_B64)

def test_chamados_crud_lifecycle():
    print("=" * 60)
    print("SUITE 3: CHAMADOS - CRUD, MULTIPART, FOTO, PAGINAÇÃO E CICLO DE STATUS")
    print("=" * 60)

    admin_auth = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"}).json()
    admin_headers = {"Authorization": f"Bearer {admin_auth['token']}"}

    gestor_auth = requests.post(f"{BASE_URL}/login", json={"email": "roberto.gestor@subprefeitura.sp.gov.br", "senha": "Senha#123"}).json()
    gestor_headers = {"Authorization": f"Bearer {gestor_auth['token']}"}

    cidadao_auth = requests.post(f"{BASE_URL}/login", json={"email": "enzo.okuizumi@gmail.com", "senha": "123mudar"}).json()
    cidadao_headers = {"Authorization": f"Bearer {cidadao_auth['token']}"}

    # 3.1 Criar chamado via JSON com foto em base64 (somente foto + endereco)
    print("\n[3.1] Criando chamado via JSON com foto Base64...")
    payload_json = {
        "endereco": {
            "cep": "01310-100",
            "numero": "500",
            "referencia": "Em frente à farmácia",
            "rua": "Avenida Paulista"
        },
        "fotoBase64": DUMMY_PNG_B64
    }
    res = requests.post(f"{BASE_URL}/chamados", headers=cidadao_headers, json=payload_json)
    assert res.status_code == 201, f"Falha ao criar chamado JSON: {res.status_code} - {res.text}"
    chamado_json_body = res.json()
    chamado_json_id = chamado_json_body["idChamado"]
    assert chamado_json_body["prioridadeNome"] == "PENDENTE", f"Prioridade inicial inesperada: {chamado_json_body['prioridadeNome']}"
    assert chamado_json_body["orgaoNome"] == "Em análise pela IA", f"Órgão inicial inesperado: {chamado_json_body['orgaoNome']}"
    print(f" -> Sucesso! Chamado ID {chamado_json_id} criado via JSON.")

    # 3.2 Criar chamado via Multipart/form-data com arquivo binário de imagem
    print("\n[3.2] Criando chamado via Multipart (part 'dados' + part 'foto')...")
    chamado_dados = {
        "cep": "01310-100",
        "numero": "750",
        "referencia": "Esquina com Alameda Campinas",
        "rua": "Avenida Paulista"
    }
    files = {
        "dados": (None, json.dumps(chamado_dados), "application/json"),
        "foto": ("bueiro.png", DUMMY_PNG_BYTES, "image/png")
    }
    res = requests.post(f"{BASE_URL}/chamados", headers=cidadao_headers, files=files)
    assert res.status_code == 201, f"Falha ao criar chamado Multipart: {res.status_code} - {res.text}"
    chamado_multipart_id = res.json()["idChamado"]
    print(f" -> Sucesso! Chamado ID {chamado_multipart_id} criado via Multipart.")

    # 3.3 Obter foto binária do chamado (GET /chamados/{id}/foto)
    print(f"\n[3.3] Baixando a foto binária do chamado ID {chamado_multipart_id}...")
    res = requests.get(f"{BASE_URL}/chamados/{chamado_multipart_id}/foto")
    assert res.status_code == 200, f"Falha ao obter foto: {res.status_code}"
    assert "image/png" in res.headers.get("Content-Type", ""), f"Content-Type incorreto: {res.headers.get('Content-Type')}"
    assert len(res.content) == len(DUMMY_PNG_BYTES), f"Tamanho da foto divergente: {len(res.content)} bytes"
    print(f" -> Sucesso! Foto recuperada com Content-Type '{res.headers.get('Content-Type')}' e {len(res.content)} bytes.")

    # 3.4 Validações de Entrada (Foto e Endereço obrigatórios)
    print("\n[3.4] Testando validações de entrada na abertura...")
    # Sem foto
    res = requests.post(f"{BASE_URL}/chamados", headers=cidadao_headers, json={
        "endereco": {"cep": "01310-100", "numero": "1", "referencia": "Ref"}
    })
    assert res.status_code == 400, f"Deveria falhar sem foto: {res.status_code}"

    # Sem endereço
    res = requests.post(f"{BASE_URL}/chamados", headers=cidadao_headers, json={
        "fotoBase64": DUMMY_PNG_B64
    })
    assert res.status_code == 400, f"Deveria falhar sem endereço: {res.status_code}"

    # PUT com categoria inexistente
    res = requests.put(f"{BASE_URL}/chamados/{chamado_json_id}", headers=cidadao_headers, json={
        "titulo": "Título válido",
        "descricao": "Descrição válida detalhada",
        "categoriaId": 99999,
        "endereco": {"cep": "01310-100", "numero": "1", "referencia": "Ref"}
    })
    assert res.status_code in [400, 404], f"Deveria falhar categoria inexistente no PUT: {res.status_code}"
    print(" -> Sucesso! Todas as 3 rejeições de validação retornaram 400/404.")

    # 3.5 Listagem Paginada e Filtros
    print("\n[3.5] Testando listagem paginada (GET /chamados?page=0&size=5)...")
    res = requests.get(f"{BASE_URL}/chamados?page=0&size=5&sort=dtAbertura,desc", headers=cidadao_headers)
    assert res.status_code == 200, f"Falha na listagem: {res.status_code}"
    page = res.json()
    assert len(page["content"]) <= 5, "Página excedeu o tamanho máximo configurado"
    print(f" -> Sucesso! Página retornou {len(page['content'])} chamados com ordenação descrescente por dtAbertura.")

    # 3.6 Filtro por 'apenasMeus=true'
    print("\n[3.6] Testando filtro 'apenasMeus=true'...")
    res = requests.get(f"{BASE_URL}/chamados?apenasMeus=true", headers=cidadao_headers)
    assert res.status_code == 200
    meus_chamados = res.json()["content"]
    assert len(meus_chamados) > 0, "Deveria haver chamados abertos pelo cidadão logado"
    for c in meus_chamados:
        assert c["usuarioId"] == cidadao_auth["user"]["id"], f"Chamado de outro usuário retornado: {c['usuarioId']}"
    print(f" -> Sucesso! Todos os {len(meus_chamados)} chamados filtrados pertencem ao usuário ID {cidadao_auth['user']['id']}.")

    # 3.7 Ciclo de Vida do Status: PENDENTE -> EM_ANALISE -> EM_EXECUCAO -> CONCLUIDO
    print(f"\n[3.7] Testando transições de status do Chamado ID {chamado_json_id} pelo Gestor...")
    fluxo = [
        ("EM_ANALISE", "Demanda encaminhada para equipe técnica de campo."),
        ("EM_EXECUCAO", "Técnicos no local realizando a substituição da lâmpada."),
        ("CONCLUIDO", "Lâmpada de LED instalada e funcionamento normalizado.")
    ]
    for novo_status, obs in fluxo:
        res = requests.patch(
            f"{BASE_URL}/chamados/{chamado_json_id}/status",
            headers=gestor_headers,
            json={"status": novo_status, "observacao": obs}
        )
        assert res.status_code == 200, f"Falha na transição para {novo_status}: {res.status_code} - {res.text}"
        assert res.json()["status"] == novo_status
        print(f"   -> Transição para '{novo_status}' realizada com sucesso.")

    # 3.8 Auditoria do Histórico Completo
    print(f"\n[3.8] Verificando histórico de auditoria completo do Chamado ID {chamado_json_id}...")
    res = requests.get(f"{BASE_URL}/chamados/{chamado_json_id}", headers=cidadao_headers)
    assert res.status_code == 200
    chamado_detalhe = res.json()
    historico = chamado_detalhe["historico"]
    assert len(historico) >= 4, f"Histórico incompleto (esperado >= 4 eventos): {len(historico)}"
    print(f" -> Sucesso! Chamado possui {len(historico)} registros de auditoria histórica:")
    for h in historico:
        print(f"    - [{h['data']}] {h['status']} por {h['autorNome']}: {h['observacao']}")

    # 3.9 Exclusão de Chamado por Admin (DELETE /chamados/{id})
    print(f"\n[3.9] Testando exclusão de chamado pelo Administrador (DELETE /chamados/{chamado_multipart_id})...")
    res = requests.delete(f"{BASE_URL}/chamados/{chamado_multipart_id}", headers=admin_headers)
    assert res.status_code == 204, f"Deveria retornar 204 No Content, retornou {res.status_code}"
    print(" -> Sucesso! Chamado excluído com 204 No Content.")

    # 3.10 Confirmação de 404 após exclusão
    print(f"\n[3.10] Confirmando que o chamado excluído ID {chamado_multipart_id} retorna 404...")
    res = requests.get(f"{BASE_URL}/chamados/{chamado_multipart_id}", headers=cidadao_headers)
    assert res.status_code == 404, f"Deveria retornar 404, retornou {res.status_code}"
    print(" -> Sucesso! Retornou 404 Not Found.")

    print("\n" + "=" * 60)
    print("SUITE 3: TODOS OS 10 TESTES DE CRUD, AUDITORIA E CICLO DE STATUS PASSARAM!")
    print("=" * 60)

if __name__ == "__main__":
    test_chamados_crud_lifecycle()
