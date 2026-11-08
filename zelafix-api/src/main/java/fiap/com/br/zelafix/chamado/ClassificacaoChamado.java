package fiap.com.br.zelafix.chamado;

import lombok.Getter;

@Getter
public enum ClassificacaoChamado {
    BURACO_VIA("Buraco na Rua", OrgaoPublico.SIURB),
    BUEIRO_ENTUPIDO("Bueiro Entupido", OrgaoPublico.SIURB),
    ILUMINACAO_PUBLICA("Iluminação Pública", OrgaoPublico.ILUME),
    ENTULHO_DESCARTE("Entulho e Descarte Irregular", OrgaoPublico.AMLURB),
    PODA_ARVORE("Poda de Árvore", OrgaoPublico.SVMA),
    SINALIZACAO_TRANSITO("Sinalização e Trânsito", OrgaoPublico.CET),
    VAZAMENTO_AGUA("Vazamento de Água", OrgaoPublico.SABESP),
    DEFESA_CIVIL("Risco Estrutural / Alagamento", OrgaoPublico.DEFESA_CIVIL),
    OUTROS("Outros", OrgaoPublico.SUBPREFEITURA);

    private final String descricao;
    private final OrgaoPublico orgaoCompetente;

    ClassificacaoChamado(String descricao, OrgaoPublico orgaoCompetente) {
        this.descricao = descricao;
        this.orgaoCompetente = orgaoCompetente;
    }

    public static ClassificacaoChamado fromString(String text) {
        if (text == null || text.isBlank()) return OUTROS;
        for (ClassificacaoChamado c : values()) {
            if (c.name().equalsIgnoreCase(text.trim()) || c.descricao.equalsIgnoreCase(text.trim())) {
                return c;
            }
        }
        return OUTROS;
    }
}

