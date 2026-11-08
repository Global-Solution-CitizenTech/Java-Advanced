package fiap.com.br.zelafix.status;

public enum StatusChamadoEnum {
    PENDENTE_TRIAGEM("Pendente Triagem"),
    ABERTO("Aberto"),
    EM_ANDAMENTO("Em Andamento"),
    CONCLUIDO("Resolvido"),
    REJEITADO("Rejeitado");

    private final String descricao;

    StatusChamadoEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static StatusChamadoEnum fromString(String text) {
        if (text == null) return ABERTO;
        String normalized = text.trim().toUpperCase().replace(" ", "_");
        for (StatusChamadoEnum s : StatusChamadoEnum.values()) {
            if (s.name().equalsIgnoreCase(normalized) || s.descricao.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        if (normalized.equals("RESOLVIDO")) return CONCLUIDO;
        if (normalized.equals("EM_ANALISE")) return ABERTO;
        return ABERTO;
    }
}

