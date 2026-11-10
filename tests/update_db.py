import oracledb

conn = oracledb.connect(
    user="RM561432",
    password="301006",
    dsn="oracle.fiap.com.br:1521/orcl"
)
cursor = conn.cursor()

cursor.execute("UPDATE usuario SET cargo = 'CIDADAO' WHERE cargo IN ('COMUM', 'PREMIUM')")
cursor.execute("UPDATE usuario SET cargo = 'GESTOR' WHERE cargo = 'ADMIN'")
conn.commit()

cursor.execute("SELECT DISTINCT cargo FROM usuario")
print("Cargos atualizados no Oracle:", cursor.fetchall())

conn.close()
