package fiap.com.br.zelafix.chamado;

import lombok.Getter;

@Getter
public enum OrgaoPublico {
    SIURB("Secretaria Municipal de Infraestrutura Urbana e Obras - SIURB"),
    ILUME("Departamento de Iluminação Pública - Ilume"),
    SABESP("Companhia de Saneamento Básico - SABESP"),
    AMLURB("Autoridade Municipal de Limpeza Urbana - AMLURB"),
    SVMA("Secretaria Municipal do Verde e do Meio Ambiente - SVMA"),
    CET("Companhia de Engenharia de Tráfego - CET"),
    DEFESA_CIVIL("Coordenação Municipal de Defesa Civil - COMDEC"),
    SUBPREFEITURA("Subprefeitura Regional");

    private final String descricao;

    OrgaoPublico(String descricao) {
        this.descricao = descricao;
    }

    public static OrgaoPublico fromString(String text) {
        if (text == null || text.isBlank()) return SUBPREFEITURA;
        for (OrgaoPublico o : values()) {
            if (o.name().equalsIgnoreCase(text.trim()) || o.descricao.equalsIgnoreCase(text.trim())) {
                return o;
            }
        }
        return SUBPREFEITURA;
    }
}

