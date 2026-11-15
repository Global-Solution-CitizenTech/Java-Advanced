import os
from pathlib import Path
import requests

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")
IMG_PATH = Path(__file__).resolve().parents[1] / "docs" / "rua-esburacada.png"

# 1. Login
r_login = requests.post(
    f"{BASE_URL}/login",
    json={"email": "enzo.okuizumi@gmail.com", "senha": "123mudar"},
    timeout=15,
)
r_login.raise_for_status()
token = r_login.json()["token"]
headers = {"Authorization": f"Bearer {token}"}

# 2. Criar chamado via multipart (POST /chamados -> RabbitMQ -> Worker Spring AI -> 201 Created)
print("\n>>> Enviando POST /chamados (multipart foto + endereco)...")
r_chamado = requests.post(
    f"{BASE_URL}/chamados",
    headers=headers,
    data={
        "cep": "01311-000",
        "rua": "Avenida Paulista",
        "numero": "1500",
        "referencia": "MASP",
    },
    files={"foto": ("rua-esburacada.png", IMG_PATH.read_bytes(), "image/png")},
    timeout=40,
)
print("Status POST /chamados:", r_chamado.status_code)
r_chamado.raise_for_status()

chamado_data = r_chamado.json()
chamado_id = chamado_data["idChamado"]
print(f"Chamado criado com ID: {chamado_id} | Titulo IA: {chamado_data['titulo']}")

r_get = requests.get(f"{BASE_URL}/chamados/{chamado_id}", headers=headers, timeout=15)
r_get.raise_for_status()
get_data = r_get.json()
print("Titulo IA:", get_data.get("titulo"))
print("Descricao IA:", get_data.get("descricao"))
print("Categoria IA:", get_data.get("categoriaNome"))
print("Prioridade IA:", get_data.get("prioridadeNome"))
print("Orgao IA:", get_data.get("orgaoNome"))
print("Status atual:", get_data.get("status"))
for h in get_data.get("historico", []):
    print(f"  - [{h.get('status')}] {h.get('observacao')} (por: {h.get('autorNome')})")
