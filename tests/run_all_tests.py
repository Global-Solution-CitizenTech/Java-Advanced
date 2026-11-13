import time
import sys

from test_01_auth_security import test_auth_and_security
from test_02_categorias_enderecos_status import test_categorias_enderecos_status
from test_03_chamados_crud_lifecycle import test_chamados_crud_lifecycle
from test_04_worker_ai_edge_cases import test_worker_ai
from test_05_stress_load import test_stress_load
from test_06_break_the_app import break_the_app

def main():
    print("#" * 70)
    print("      ZELAFIX - BATERIA GERAL DE TESTES DE ESTRESSE E INTEGRAÇÃO")
    print("      Testando API (Porta 8080) e Worker Stateless RabbitMQ (Porta 8081)")
    print("#" * 70)

    start_total = time.time()
    suites = [
        ("Suite 1: Autenticação, Registro e RBAC", test_auth_and_security),
        ("Suite 2: Categorias, Status e Endereços", test_categorias_enderecos_status),
        ("Suite 3: Chamados CRUD, Fotos e Auditoria", test_chamados_crud_lifecycle),
        ("Suite 4: Worker Spring AI (Azure AI Foundry) e Resiliência", test_worker_ai),
        ("Suite 5: Concorrência e Carga de Votos/Leituras", test_stress_load),
        ("Suite 6: Stress Máximo ao Limite (500 reqs concorrentes)", break_the_app)
    ]

    for nome, fn in suites:
        t0 = time.time()
        print(f"\n>>> INICIANDO {nome}...")
        try:
            fn()
            print(f">>> {nome} FINALIZADA COM SUCESSO em {time.time() - t0:.2f}s!")
        except Exception as e:
            print(f">>> FALHA NA {nome}: {e}")
            sys.exit(1)

    duracao = time.time() - start_total
    print("\n" + "#" * 70)
    print(f"      TODAS AS 6 SUÍTES FORAM CONCLUÍDAS COM 100% DE SUCESSO!")
    print(f"      Tempo Total de Execução: {duracao:.2f} segundos")
    print("#" * 70)

if __name__ == "__main__":
    main()
