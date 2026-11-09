package fiap.com.br.zelafix.usuario;

public enum CargoUsuario {
    CIDADAO,
    GESTOR;

    public boolean isGestor() {
        return this == GESTOR;
    }
}
