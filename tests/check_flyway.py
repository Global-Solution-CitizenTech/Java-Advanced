import oracledb

conn = oracledb.connect(user='RM561432', password='301006', dsn='oracle.fiap.com.br:1521/orcl')
cursor = conn.cursor()
cursor.execute("SELECT id_hist, chamado_id_chamado, status_id_status, observacoes, dt_mudanca FROM hist_chamado WHERE chamado_id_chamado = 59 ORDER BY id_hist ASC")
for r in cursor.fetchall():
    print('Hist:', r)
conn.close()
