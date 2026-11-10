import requests
import json
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
import collections

BASE_URL = "http://localhost:8080"

def test_stress_load():
    print("=" * 60)
    print("SUITE 5: TESTE DE CARGA EXTREMA, CONCORRÊNCIA E LIMITES DE STRESS")
    print("=" * 60)

    # 1. Autenticar múltiplos usuários para simular concorrência real
    print("\n[5.1] Autenticando usuários para o teste de concorrência...")
    tokens = []
    creds = [
        ("admin@zelafix.com.br", "123mudar"),
        ("roberto.gestor@subprefeitura.sp.gov.br", "Senha#123"),
        ("carlos.silva@email.com", "Senha#123"),
        ("mariana.souza@email.com", "Senha#123"),
        ("fernanda.costa@email.com", "Senha#123"),
        ("lucas.zeladoria@subprefeitura.sp.gov.br", "Senha#123")
    ]
    for email, senha in creds:
        try:
            r = requests.post(f"{BASE_URL}/login", json={"email": email, "senha": senha}, timeout=5)
            if r.status_code == 200:
                tokens.append((email, r.json()["token"]))
        except Exception as e:
            print(f"Erro ao logar {email}: {e}")

    print(f" -> {len(tokens)} contas autenticadas para simular chamadas simultâneas.")
    admin_token = tokens[0][1]

    # Criar um chamado alvo para a tempestade de votos
    print("\n[5.2] Criando chamado alvo para rajada massiva de votos...")
    target_chamado = requests.post(
        f"{BASE_URL}/chamados",
        headers={"Authorization": f"Bearer {admin_token}"},
        json={
            "titulo": "Buraco de Stress Test Concorrente",
            "descricao": "Chamado para avaliação de concorrência massiva de votos e mensageria RabbitMQ.",
            "categoriaId": 1,
            "endereco": {"cep": "01310-100", "numero": "999", "referencia": "Ponto Zero", "rua": "Paulista"}
        }
    ).json()
    target_id = target_chamado["idChamado"]
    print(f" -> Chamado alvo ID: {target_id}")

    # =========================================================================
    # STRESS TEST 1: VOTOS CONCORRENTES EM ALTA VELOCIDADE (100 requisições simultâneas)
    # =========================================================================
    print(f"\n[5.3] DISPARANDO TEMPESTADE DE VOTOS CONCORRENTES: 100 requisições em 10 threads...")
    
    def enviar_voto(indice):
        user_email, token = tokens[indice % len(tokens)]
        start = time.time()
        try:
            res = requests.post(
                f"{BASE_URL}/chamados/{target_id}/apoiar",
                headers={"Authorization": f"Bearer {token}"},
                timeout=15
            )
            elapsed = time.time() - start
            return res.status_code, elapsed, None
        except Exception as ex:
            elapsed = time.time() - start
            return 0, elapsed, str(ex)

    resultados_votos = collections.Counter()
    latencias_votos = []
    erros_votos = []

    start_batch = time.time()
    with ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(enviar_voto, i) for i in range(100)]
        for f in as_completed(futures):
            status, lat, err = f.result()
            resultados_votos[status] += 1
            latencias_votos.append(lat)
            if err:
                erros_votos.append(err)

    total_time_votos = time.time() - start_batch
    avg_lat = sum(latencias_votos) / len(latencias_votos) if latencias_votos else 0
    max_lat = max(latencias_votos) if latencias_votos else 0

    print(f" -> Resultados Votos: {dict(resultados_votos)}")
    print(f" -> Tempo total: {total_time_votos:.2f}s | Vazão: {100 / total_time_votos:.1f} req/s")
    print(f" -> Latência Média: {avg_lat*1000:.1f}ms | Máxima: {max_lat*1000:.1f}ms")
    if erros_votos:
        print(f" -> Exceções capturadas: {len(erros_votos)} (primeiro erro: {erros_votos[0]})")

    # =========================================================================
    # STRESS TEST 2: CRIAÇÃO CONCORRENTE DE CHAMADOS (20 chamados simultâneos)
    # =========================================================================
    print(f"\n[5.4] DISPARANDO CRIAÇÃO CONCORRENTE DE CHAMADOS: 20 chamados simultâneos...")

    def criar_chamado_stress(indice):
        user_email, token = tokens[indice % len(tokens)]
        start = time.time()
        try:
            res = requests.post(
                f"{BASE_URL}/chamados",
                headers={"Authorization": f"Bearer {token}"},
                json={
                    "titulo": f"Chamado de Stress #{indice}",
                    "descricao": f"Descrição de teste de carga concorrente número {indice} para saturar banco e mensageria.",
                    "categoriaId": 1,
                    "endereco": {"cep": "01310-100", "numero": str(1000 + indice), "referencia": f"Lote {indice}", "rua": "Paulista"}
                },
                timeout=20
            )
            elapsed = time.time() - start
            return res.status_code, elapsed, res.text if res.status_code != 201 else None
        except Exception as ex:
            elapsed = time.time() - start
            return 0, elapsed, str(ex)

    resultados_criacao = collections.Counter()
    latencias_criacao = []
    falhas_criacao = []

    start_batch_criacao = time.time()
    with ThreadPoolExecutor(max_workers=8) as executor:
        futures = [executor.submit(criar_chamado_stress, i) for i in range(20)]
        for f in as_completed(futures):
            status, lat, err_msg = f.result()
            resultados_criacao[status] += 1
            latencias_criacao.append(lat)
            if status != 201:
                falhas_criacao.append((status, err_msg))

    total_time_criacao = time.time() - start_batch_criacao
    avg_lat_c = sum(latencias_criacao) / len(latencias_criacao) if latencias_criacao else 0

    print(f" -> Resultados Criação: {dict(resultados_criacao)}")
    print(f" -> Tempo total: {total_time_criacao:.2f}s | Vazão: {20 / total_time_criacao:.1f} req/s")
    print(f" -> Latência Média: {avg_lat_c*1000:.1f}ms")
    if falhas_criacao:
        print(f" -> Falhas na criação: {len(falhas_criacao)} falhas:")
        for st, msg in falhas_criacao[:3]:
            print(f"    - Status {st}: {msg[:150]}")

    # =========================================================================
    # STRESS TEST 3: LEITURA MASSIVA CONCORRENTE COM CARREGAMENTO EM LOTE (150 consultas)
    # =========================================================================
    print(f"\n[5.5] DISPARANDO 150 CONSULTAS CONCORRENTES EM GET /chamados COM PAGINAÇÃO...")

    def consultar_chamados(indice):
        token = tokens[indice % len(tokens)][1]
        start = time.time()
        try:
            res = requests.get(
                f"{BASE_URL}/chamados?page={indice % 3}&size=10&sort=dtAbertura,desc",
                headers={"Authorization": f"Bearer {token}"},
                timeout=10
            )
            elapsed = time.time() - start
            return res.status_code, elapsed
        except Exception:
            return 0, time.time() - start

    resultados_leitura = collections.Counter()
    latencias_leitura = []

    start_leitura = time.time()
    with ThreadPoolExecutor(max_workers=15) as executor:
        futures = [executor.submit(consultar_chamados, i) for i in range(150)]
        for f in as_completed(futures):
            st, lat = f.result()
            resultados_leitura[st] += 1
            latencias_leitura.append(lat)

    total_time_leitura = time.time() - start_leitura
    avg_lat_l = sum(latencias_leitura) / len(latencias_leitura) if latencias_leitura else 0
    print(f" -> Resultados Leitura: {dict(resultados_leitura)}")
    print(f" -> Vazão Leitura: {150 / total_time_leitura:.1f} req/s | Latência Média: {avg_lat_l*1000:.1f}ms")

    print("\n" + "=" * 60)
    print("SUITE 5 CONCLUÍDA! ANALISANDO ESTABILIDADE DOS SERVIÇOS...")
    print("=" * 60)

if __name__ == "__main__":
    test_stress_load()
