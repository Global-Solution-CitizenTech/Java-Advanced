import oracledb

conn = oracledb.connect(user='RM561432', password='301006', dsn='oracle.fiap.com.br:1521/orcl')
cursor = conn.cursor()
cursor.execute("DELETE FROM FLYWAY_ZELAFIX_HISTORY WHERE \"version\" = '3'")
print(f'Deleted {cursor.rowcount} failed V3 rows from FLYWAY_ZELAFIX_HISTORY')
conn.commit()
conn.close()
