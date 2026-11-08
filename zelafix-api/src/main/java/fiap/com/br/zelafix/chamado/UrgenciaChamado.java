package fiap.com.br.zelafix.chamado;

public enum UrgenciaChamado {
    BAIXA("Baixa"),
    MEDIA("Média"),
    ALTA("Alta"),
    CRITICA("Crítica");

    private final String descricao;

    UrgenciaChamado(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static UrgenciaChamado fromString(String text) {
        if (text == null) return MEDIA;
        String normalized = text.trim().toUpperCase().replace("É", "E").replace("Í", "I");
        for (UrgenciaChamado u : UrgenciaChamado.values()) {
            if (u.name().equalsIgnoreCase(normalized) || u.descricao.equalsIgnoreCase(text.trim())) {
                return u;
            }
        }
        return MEDIA;
    }
}

