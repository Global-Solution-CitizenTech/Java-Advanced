package fiap.com.br.zelafix.usuario;

public enum CargoUsuario {
    CIDADAO,
    GESTOR,
    COMUM,
    PREMIUM,
    ADMIN;

    public boolean isGestor() {
        return this == GESTOR || this == ADMIN;
    }
}
