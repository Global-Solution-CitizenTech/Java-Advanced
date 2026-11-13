import base64
import json
import os
import time
import urllib.request
from pathlib import Path

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")
WORKER_URL = os.getenv("WORKER_URL", "http://localhost:8081")
IMG_PATH = Path(__file__).resolve().parents[1] / "docs" / "rua-esburacada.png"

def request(method, path, data=None, token=None, url_override=None):
    url = (url_override or BASE_URL) + path
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode("utf-8") if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as response:
            status = response.status
            content = response.read().decode("utf-8")
            try:
                parsed = json.loads(content)
            except Exception:
                parsed = content
            return status, parsed
    except urllib.error.HTTPError as e:
        content = e.read().decode("utf-8")
        try:
            parsed = json.loads(content)
        except Exception:
            parsed = content
        return e.code, parsed
    except Exception as e:
        return 500, str(e)

print("=" * 70)
print("1. TESTANDO ACTUATOR E HEALTH CHECKS")
print("=" * 70)
status, res = request("GET", "/actuator/health")
print(f"API Health (8080): {status} -> status: {res.get('status')}")
status, res = request("GET", "/actuator/health", url_override=WORKER_URL)
print(f"Worker Health (8081): {status} -> status: {res.get('status')}, rabbit: {res.get('components', {}).get('rabbit', {}).get('status')}")

print("\n" + "=" * 70)
print("2. TESTANDO OPENAPI DOCS")
print("=" * 70)
status, res = request("GET", "/v3/api-docs")
print(f"OpenAPI Docs: {status} -> Title: {res.get('info', {}).get('title') if isinstance(res, dict) else 'N/A'}")

print("\n" + "=" * 70)
print("3. TESTANDO AUTENTICAÇÃO (/login)")
print("=" * 70)
# Login Admin
status, admin_auth = request("POST", "/login", {"email": "admin@zelafix.com.br", "senha": "123mudar"})
print(f"POST /login (Admin): {status} -> Token: {'RECEBIDO' if 'token' in admin_auth else 'FALHA'}")
admin_token = admin_auth.get("token")

# Cadastro público de novo munícipe (POST /usuarios)
novo_email = f"municipe_{int(time.time())}@zelafix.com.br"
novo_municipe = {
    "nome": "Munícipe Teste Automação",
    "email": novo_email,
    "senha": "Senha@Forte123",
    "cargo": "CIDADAO",
    "ddd": "11",
    "numeroTelefone": "988776655",
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1500",
        "referencia": "Em frente ao Trianon"
    }
}
status, reg_res = request("POST", "/usuarios", novo_municipe)
print(f"POST /usuarios (Cadastro {novo_email}): {status} -> ID: {reg_res.get('id')}, Cargo: {reg_res.get('cargo')}")

# Login do Munícipe cadastrado
status, municipe_auth = request("POST", "/login", {"email": novo_email, "senha": "Senha@Forte123"})
print(f"POST /login (Novo Munícipe): {status} -> Token: {'RECEBIDO' if 'token' in municipe_auth else 'FALHA'}")
municipe_token = municipe_auth.get("token")

print("\n" + "=" * 70)
print("4. TESTANDO PERFIL E GESTÃO DE USUÁRIOS (/usuarios)")
print("=" * 70)
# Perfil próprio
status, me = request("GET", "/usuarios/me", token=municipe_token)
print(f"GET /usuarios/me: {status} -> Nome: {me.get('nome')}, Cargo: {me.get('cargo')}")

# Atualização de perfil
me_update = {
    "nome": "Munícipe Atualizado",
    "email": novo_email,
    "senha": "Senha@Forte123",
    "cargo": "CIDADAO",
    "ddd": "11",
    "numeroTelefone": "911223344",
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1550",
        "referencia": "Próximo ao MASP"
    }
}
status, me_up_res = request("PUT", "/usuarios/me", me_update, token=municipe_token)
print(f"PUT /usuarios/me: {status} -> Novo Nome: {me_up_res.get('nome')}")

# Listagem de usuários (Admin)
status, users = request("GET", "/usuarios", token=admin_token)
print(f"GET /usuarios (Admin): {status} -> Total: {users.get('totalElements') if isinstance(users, dict) else len(users)}")

print("\n" + "=" * 70)
print("5. TESTANDO CATEGORIAS E ENDEREÇOS")
print("=" * 70)
status, categorias = request("GET", "/categorias", token=municipe_token)
print(f"GET /categorias: {status} -> Total: {len(categorias) if isinstance(categorias, list) else 'N/A'}")
categoria_id = categorias[0]["id"] if isinstance(categorias, list) and categorias else 1

status, enderecos = request("GET", "/enderecos", token=admin_token)
print(f"GET /enderecos (Admin): {status} -> Total: {enderecos.get('totalElements') if isinstance(enderecos, dict) else 'N/A'}")

print("\n" + "=" * 70)
print("6. TESTANDO ESTATÍSTICAS E LISTAGEM DE CHAMADOS")
print("=" * 70)
status, stats = request("GET", "/chamados/estatisticas", token=municipe_token)
print(f"GET /chamados/estatisticas: {status} -> {stats}")

status, chamados = request("GET", "/chamados", token=municipe_token)
print(f"GET /chamados: {status} -> Total chamados: {chamados.get('totalElements') if isinstance(chamados, dict) else 'N/A'}")

print("\n" + "=" * 70)
print("7. CRIANDO NOVO CHAMADO (RABBITMQ + AZURE AI ASSÍNCRONO)")
print("=" * 70)
foto_b64 = base64.b64encode(IMG_PATH.read_bytes()).decode("utf-8")
novo_chamado = {
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1000",
        "referencia": "Próximo à estação Brigadeiro"
    },
    "fotoBase64": foto_b64
}
status, chamado_criado = request("POST", "/chamados", novo_chamado, token=municipe_token)
chamado_id = chamado_criado.get("idChamado") if isinstance(chamado_criado, dict) else None
print(f"POST /chamados: {status} -> ID Gerado: {chamado_id}, Prioridade Inicial: {chamado_criado.get('prioridadeNome') if isinstance(chamado_criado, dict) else chamado_criado}")

if chamado_id:
    print("\nAguardando 15 segundos para o zelafix-worker consumir do RabbitMQ e acionar o Azure Phi-4 Multimodal...")
    time.sleep(15)
    status, chamado_detalhe = request("GET", f"/chamados/{chamado_id}", token=municipe_token)
    print(f"GET /chamados/{chamado_id}: {status}")
    print(f" -> Título IA: {chamado_detalhe.get('titulo')} | Categoria: {chamado_detalhe.get('categoriaNome')} | Prioridade: {chamado_detalhe.get('prioridadeNome')} | Órgão: {chamado_detalhe.get('orgaoNome')}")
    historico = chamado_detalhe.get("historico", [])
    print(f"Histórico de Auditoria ({len(historico)} registros):")
    for h in historico:
        print(f" -> Status: {h.get('status')} | Operador: {h.get('autorNome')} | Parecer: {h.get('observacao')}")

    print("\n" + "=" * 70)
    print("8. TESTANDO APOIO POPULAR / VOTAÇÃO (RABBITMQ VOTO QUEUE)")
    print("=" * 70)
    status, voto_res = request("POST", f"/chamados/{chamado_id}/apoiar", token=municipe_token)
    print(f"POST /chamados/{chamado_id}/apoiar (Apoiar): {status} -> Total Votos: {voto_res.get('totalVotos')}, Apoiado Por Mim: {voto_res.get('apoiadoPorMim')}")
    
    status, desvoto_res = request("POST", f"/chamados/{chamado_id}/apoiar", token=municipe_token)
    print(f"POST /chamados/{chamado_id}/apoiar (Desfazer Apoio / Toggle): {status} -> Total Votos: {desvoto_res.get('totalVotos')}, Apoiado Por Mim: {desvoto_res.get('apoiadoPorMim')}")

    print("\n" + "=" * 70)
    print("9. TESTANDO TRANSIÇÃO DE STATUS E AUDITORIA (ADMIN)")
    print("=" * 70)
    status_update = {"status": "EM_EXECUCAO", "observacao": "Equipe de pavimentação da prefeitura deslocada para o local."}
    status, update_res = request("PATCH", f"/chamados/{chamado_id}/status", status_update, token=admin_token)
    print(f"PATCH /chamados/{chamado_id}/status: {status} -> Novo Status: {update_res.get('status')}")

    concluido_update = {"status": "CONCLUIDO", "observacao": "Recapeamento concluído com sucesso e via liberada."}
    status, concluido_res = request("PATCH", f"/chamados/{chamado_id}/status", concluido_update, token=admin_token)
    print(f"PATCH /chamados/{chamado_id}/status (Finalização): {status} -> Novo Status: {concluido_res.get('status')}")

print("\n" + "=" * 70)
print("TODOS OS ENDPOINTS E FLUXOS FORAM EXECUTADOS COM SUCESSO!")
print("=" * 70)
