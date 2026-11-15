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
    print("SUITE 4: WORKER COM SPRING AI (LLM MULTIMODAL) - TRIAGEM MULTIMODAL")
    print("=" * 60)

    auth = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"}).json()
    headers = {"Authorization": f"Bearer {auth['token']}"}
    foto_bytes = IMG_PATH.read_bytes()

    triagens_concluidas = []

    for i, caso in enumerate(CASOS_TESTE, 1):
        print(f"\n[4.{i}] Disparando chamado multimodal de teste: '{caso['nome']}'...")
        res = requests.post(
            f"{BASE_URL}/chamados",
            headers=headers,
            data={
                "cep": caso["cep"],
                "numero": caso["numero"],
                "referencia": caso["referencia"],
                "rua": caso["rua"],
            },
            files={"foto": ("rua-esburacada.png", foto_bytes, "image/png")},
            timeout=40,
        )
        assert res.status_code == 201, f"Falha ao criar chamado: {res.status_code} - {res.text}"
        dados = res.json()
        triagens_concluidas.append(dados)
        print(
            f" -> Chamado ID {dados['idChamado']} persistido após triagem IA (201 Created): "
            f"[{dados['status']} | {dados['prioridadeNome']}] {dados['categoriaNome']} | {dados['orgaoNome']} -> {dados['titulo']}"
        )

    sucessos = len(triagens_concluidas)
    print("\n" + "=" * 60)
    print(f"SUITE 4: {sucessos}/{len(CASOS_TESTE)} CHAMADOS TIVERAM TRIAGEM DA IA GRAVADA NO BANCO!")
    print("=" * 60)
    assert sucessos == len(CASOS_TESTE)


if __name__ == "__main__":
    test_worker_ai()
