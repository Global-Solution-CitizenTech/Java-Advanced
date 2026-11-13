from pathlib import Path
import base64
import time
import requests

BASE_URL = "https://app-zelafix-561432.azurewebsites.net"
IMG_PATH = Path(__file__).resolve().parents[3] / "docs" / "rua-esburacada.png"

# 1. Login
r_login = requests.post(
    f"{BASE_URL}/login",
    json={"email": "enzo.okuizumi@gmail.com", "senha": "123mudar"},
    timeout=15,
)
r_login.raise_for_status()
token = r_login.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Carregar imagem real em Base64 (sem prefixo data URI)
img_b64 = base64.b64encode(IMG_PATH.read_bytes()).decode("utf-8")

# 3. Criar chamado assincrono apenas com Foto + Endereco (POST /chamados -> RabbitMQ -> Worker Spring AI)
payload = {
    "fotoBase64": img_b64,
    "endereco": {
        "cep": "01311000",
        "rua": "Avenida Paulista",
        "numero": "1500",
        "referencia": "MASP",
    },
}

print("\n>>> Enviando POST /chamados (somente foto + endereco)...")
r_chamado = requests.post(f"{BASE_URL}/chamados", headers=headers, json=payload, timeout=30)
print("Status POST /chamados:", r_chamado.status_code)
chamado_data = r_chamado.json()
chamado_id = chamado_data.get("idChamado")
print(f"Chamado criado com ID: {chamado_id} | Titulo inicial: {chamado_data.get('titulo')}")

print("\n>>> Aguardando 15s para o Worker processar no RabbitMQ...")
time.sleep(15)

r_get = requests.get(f"{BASE_URL}/chamados/{chamado_id}", headers=headers, timeout=15)
get_data = r_get.json()
print("Titulo IA:", get_data.get("titulo"))
print("Descricao IA:", get_data.get("descricao"))
print("Categoria IA:", get_data.get("categoriaNome"))
print("Prioridade IA:", get_data.get("prioridadeNome"))
print("Orgao IA:", get_data.get("orgaoNome"))
print("Status atual:", get_data.get("status"))
for h in get_data.get("historico", []):
    print(f"  - [{h.get('status')}] {h.get('observacao')} (por: {h.get('autorNome')})")
