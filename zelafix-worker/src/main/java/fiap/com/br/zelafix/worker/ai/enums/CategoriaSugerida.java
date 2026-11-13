package fiap.com.br.zelafix.worker.ai.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CategoriaSugerida {
    ILUMINACAO_PUBLICA("Iluminação Pública"),
    VIAS_E_PAVIMENTACAO("Vias e Pavimentação"),
    DRENAGEM_E_ESGOTO("Drenagem e Esgoto"),
    LIMPEZA_URBANA("Limpeza Urbana"),
    ARVORES_E_PARQUES("Árvores e Parques"),
    SINALIZACAO_E_TRANSITO("Sinalização e Trânsito");

    private final String nomeCategoria;
}
