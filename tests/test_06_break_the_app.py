import requests
import json
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
import collections

BASE_URL = "http://localhost:8080"

def break_the_app():
    print("=" * 60)
    print("SUITE 6: QUEBRANDO A APLICAÇÃO - CARGA MÁXIMA DE STRESS")
    print("=" * 60)

    # 1. Login e obtenção de chamado válido para votos
    admin_auth = requests.post(f"{BASE_URL}/login", json={"email": "admin@zelafix.com.br", "senha": "123mudar"}).json()
    token = admin_auth["token"]
    headers = {"Authorization": f"Bearer {token}"}
    chamados_resp = requests.get(f"{BASE_URL}/chamados?size=1", headers=headers, timeout=10).json()
    target_chamado_id = chamados_resp["content"][0]["idChamado"]

    # 2. Rajada de 500 Votos Concorrentes em 30 Threads
    print(f"\n[6.1] STRESS DE VOTOS: 500 requisições simultâneas em 30 threads paralelas no chamado #{target_chamado_id}...")
    def votar(idx):
        start = time.time()
        try:
            r = requests.post(f"{BASE_URL}/chamados/{target_chamado_id}/apoiar", headers=headers, timeout=20)
            return r.status_code, time.time() - start, None
        except Exception as e:
            return 0, time.time() - start, str(e)

    res_votos = collections.Counter()
    latencias_v = []
    erros_v = []

    t0 = time.time()
    with ThreadPoolExecutor(max_workers=30) as executor:
        futures = [executor.submit(votar, i) for i in range(500)]
        for f in as_completed(futures):
            st, lat, err = f.result()
            res_votos[st] += 1
            latencias_v.append(lat)
            if err:
                erros_v.append(err)

    t_total_v = time.time() - t0
    print(f" -> Resultados (500 votos): {dict(res_votos)}")
    print(f" -> Tempo: {t_total_v:.2f}s | Vazão: {500/t_total_v:.1f} req/s")
    print(f" -> Latência Média: {(sum(latencias_v)/len(latencias_v))*1000:.1f}ms | Máxima: {max(latencias_v)*1000:.1f}ms")
    if erros_v:
        print(f" -> Erros: {len(erros_v)} (Ex: {erros_v[0]})")

    # 3. Rajada de 50 Criações Concorrentes com foto em Base64
    print("\n[6.2] STRESS DE CRIAÇÃO: 50 chamados simultâneos com foto Base64 em 15 threads...")
    DUMMY_B64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    def criar(idx):
        start = time.time()
        try:
            r = requests.post(f"{BASE_URL}/chamados", headers=headers, json={
                "endereco": {"cep": "01310-100", "numero": str(2000 + idx), "referencia": f"Stress {idx}", "rua": "Paulista"},
                "fotoBase64": DUMMY_B64
            }, timeout=25)
            return r.status_code, time.time() - start, r.text if r.status_code != 201 else None
        except Exception as e:
            return 0, time.time() - start, str(e)

    res_cria = collections.Counter()
    latencias_c = []
    falhas_c = []

    t0_c = time.time()
    with ThreadPoolExecutor(max_workers=15) as executor:
        futures = [executor.submit(criar, i) for i in range(50)]
        for f in as_completed(futures):
            st, lat, err = f.result()
            res_cria[st] += 1
            latencias_c.append(lat)
            if st != 201:
                falhas_c.append((st, err))

    t_total_c = time.time() - t0_c
    print(f" -> Resultados (50 criações): {dict(res_cria)}")
    print(f" -> Tempo: {t_total_c:.2f}s | Vazão: {50/t_total_c:.1f} req/s")
    print(f" -> Latência Média: {(sum(latencias_c)/len(latencias_c))*1000:.1f}ms | Máxima: {max(latencias_c)*1000:.1f}ms")
    if falhas_c:
        print(f" -> Falhas: {len(falhas_c)} falhas:")
        for st, err in falhas_c[:3]:
            print(f"    - Status {st}: {err[:150]}")

    print("\n" + "=" * 60)
    print("TESTE MÁXIMO DE STRESS CONCLUÍDO!")
    print("=" * 60)

if __name__ == "__main__":
    break_the_app()
