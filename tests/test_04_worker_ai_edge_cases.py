import base64
import time
from pathlib import Path
import requests

BASE_URL = "http://localhost:8080"
IMG_PATH = Path(__file__).resolve().parents[1] / "docs" / "rua-esburacada.png"

CASOS_TESTE = [
    {
        "nome": "Pavimentação - Análise Multimodal (Bela Vista)",
        "cep": "01310-100",
        "numero": "1000",
        "referencia": "Em frente ao MASP",
        "rua": "Avenida Paulista",
    },
    {
        "nome": "Pavimentação - Análise Multimodal (Pinheiros)",
        "cep": "05422-000",
        "numero": "450",
        "referencia": "Esquina com Fradique Coutinho",
        "rua": "Rua dos Pinheiros",
    },
]


def test_worker_ai():
    print("=" * 60)
    print("SUITE 4: WORKER COM SPRING AI (AZURE AI FOUNDRY) - TRIAGEM MULTIMODAL")
    print("=" * 60)

    auth = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"}).json()
    headers = {"Authorization": f"Bearer {auth['token']}"}
    foto_b64 = base64.b64encode(IMG_PATH.read_bytes()).decode("utf-8")

    chamados_criados = []

    for i, caso in enumerate(CASOS_TESTE, 1):
        print(f"\n[4.{i}] Disparando chamado multimodal de teste: '{caso['nome']}'...")
        payload = {
            "endereco": {
                "cep": caso["cep"],
                "numero": caso["numero"],
                "referencia": caso["referencia"],
                "rua": caso["rua"],
            },
            "fotoBase64": foto_b64,
        }
        res = requests.post(f"{BASE_URL}/chamados", headers=headers, json=payload, timeout=30)
        assert res.status_code == 202, f"Falha ao criar chamado: {res.status_code} - {res.text}"
        chamados_criados.append((caso["numero"], caso))
        print(f" -> Solicitação para número {caso['numero']} aceita (202 Accepted) e publicada no RabbitMQ.")

    print(f"\nTodos os {len(chamados_criados)} chamados foram despachados!")
    print("Aguardando processamento do Worker com Spring AI e persistência no banco...")

    timeout_max = 90
    start_wait = time.time()
    chamados_pendentes = dict(chamados_criados)
    triagens_concluidas = {}

    while chamados_pendentes and (time.time() - start_wait) < timeout_max:
        time.sleep(3)
        res = requests.get(f"{BASE_URL}/chamados?apenasMeus=true&size=20&sort=dtAbertura,desc", headers=headers, timeout=15)
        if res.status_code == 200:
            for dados in res.json().get("content", []):
                num = dados.get("endereco", {}).get("numero")
                if num in chamados_pendentes:
                    triagens_concluidas[num] = dados
                    del chamados_pendentes[num]
                    print(
                        f" -> Chamado ID {dados['idChamado']} persistido em {time.time() - start_wait:.1f}s: "
                        f"[{dados['status']} | {dados['prioridadeNome']}] {dados['categoriaNome']} | {dados['orgaoNome']} -> {dados['titulo']}"
                    )

    sucessos = len(triagens_concluidas)
    if chamados_pendentes:
        print(f"\nALERTA: {len(chamados_pendentes)} chamados não concluíram a triagem em {timeout_max}s:")
        for cid, caso in chamados_pendentes.items():
            print(f"    - ID {cid}: {caso['nome']}")

    print("\n" + "=" * 60)
    print(f"SUITE 4: {sucessos}/{len(chamados_criados)} CHAMADOS TIVERAM TRIAGEM DA IA GRAVADA NO BANCO!")
    print("=" * 60)
    assert sucessos == len(chamados_criados), f"Nem todos os chamados receberam o callback do worker ({sucessos}/{len(chamados_criados)})"


if __name__ == "__main__":
    test_worker_ai()
