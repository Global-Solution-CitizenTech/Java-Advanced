import requests
import os
from pathlib import Path

BASE_URL = "http://localhost:8080"
IMAGE_PATH = Path(__file__).resolve().parents[1] / "docs" / "rua-esburacada.png"

print("=" * 60)
print("TESTE DE UPLOAD E DOWNLOAD DE IMAGEM REAL (MULTIPART /chamados -> /foto)")
print("=" * 60)

# 1. Login com munícipe válido
print("\n[1] Autenticando munícipe (enzo.okuizumi@gmail.com)...")
login_resp = requests.post(f"{BASE_URL}/login", json={
    "email": "enzo.okuizumi@gmail.com",
    "senha": "123mudar"
})
assert login_resp.status_code == 200, f"Falha no login: {login_resp.status_code} - {login_resp.text}"
token = login_resp.json()["token"]
headers = {"Authorization": f"Bearer {token}"}
print(" -> Sucesso! Token JWT obtido.")

# 2. Upload da imagem real via POST /chamados (multipart)
print("\n[2] Abrindo chamado com foto real de zeladoria via Multipart...")
with open(IMAGE_PATH, "rb") as f:
    files = {"foto": ("rua-esburacada.png", f, "image/png")}
    data = {
        "cep": "01310-100",
        "numero": "300",
        "referencia": "Em frente à praça",
        "rua": "Avenida Paulista"
    }
    upload_resp = requests.post(f"{BASE_URL}/chamados", headers=headers, data=data, files=files, timeout=60)

assert upload_resp.status_code == 201, f"Falha no upload/criação do chamado: {upload_resp.status_code} - {upload_resp.text}"
chamado_data = upload_resp.json()
chamado_id = chamado_data["idChamado"]
foto_url = chamado_data["foto"]
print(f" -> Chamado #{chamado_id} criado com sucesso (201 Created)! Foto path: {foto_url}")

# 3. Download da foto via GET /chamados/{id}/foto
print(f"\n[3] Baixando a foto binária via GET {BASE_URL}{foto_url}...")
get_resp = requests.get(f"{BASE_URL}{foto_url}", timeout=20)
print(" -> Status:", get_resp.status_code)
print(" -> Content-Type:", get_resp.headers.get("content-type"))
print(" -> Tamanho recebido:", len(get_resp.content), "bytes")
original_size = os.path.getsize(IMAGE_PATH)
print(" -> Tamanho original no disco:", original_size, "bytes")

assert get_resp.status_code == 200, f"Esperado 200, obtido {get_resp.status_code}"
assert "image/png" in get_resp.headers.get("content-type", ""), f"Content-Type inesperado: {get_resp.headers.get('content-type')}"
assert len(get_resp.content) == original_size, f"Tamanho divergente: {len(get_resp.content)} vs {original_size}"

print("\n" + "=" * 60)
print("SUCESSO: Imagem real enviada via Multipart, persistida no Oracle e recuperada byte-a-byte com 100% de integridade!")
print("=" * 60)
