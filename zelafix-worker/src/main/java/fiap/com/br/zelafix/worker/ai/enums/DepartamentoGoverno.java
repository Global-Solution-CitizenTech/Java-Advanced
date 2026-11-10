package fiap.com.br.zelafix.worker.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DepartamentoGoverno {
    SECRETARIA_OBRAS("Secretaria Municipal de Infraestrutura Urbana e Obras"),
    CET_TRANSITO("Companhia de Engenharia de Tráfego (CET)"),
    ILUMINACAO_PUBLICA("Departamento de Iluminação Pública (Ilume)"),
    DEFESA_CIVIL("Coordenação Municipal de Defesa Civil"),
    SUBPREFEITURA("Subprefeitura Regional"),
    SANEAMENTO_BASICO("Companhia de Saneamento Básico e Drenagem");

    private final String nomeOficial;
}
