package fiap.com.br.zelafix.worker.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;

import java.nio.charset.StandardCharsets;

public record TriagemIaResult(
        @JsonPropertyDescription("true somente se a foto mostrar uma ocorrência real de zeladoria em espaço público urbano (rua, calçada, praça, poste, bueiro, semáforo); false se for captura de tela, jogo, anime, ilustração, meme, selfie, pessoa, animal, objeto ou ambiente interno")
        boolean ocorrenciaValida,

        @JsonPropertyDescription("Título curto, direto e em português do Brasil natural com capitalização normal de frase, sem usar tudo em maiúsculas (máximo 70 caracteres para respeitar o limite de bytes UTF-8 do Oracle)")
        String titulo,

        @JsonPropertyDescription("Descrição objetiva em português do Brasil explicando o problema visível na foto, seu impacto na via/calçada, o reparo necessário e, ao final, o decreto e o prazo SLA retornados pelas ferramentas (máximo 500 caracteres para respeitar o limite de bytes UTF-8 do Oracle)")
        String descricao,

        @JsonPropertyDescription("Categoria oficial do problema urbano observado na foto")
        CategoriaSugerida categoriaSugerida,

        @JsonPropertyDescription("Nível de prioridade da ocorrência urbana")
        PrioridadeChamado prioridade,

        @JsonPropertyDescription("Órgão ou secretaria municipal competente para a resolução")
        DepartamentoGoverno orgaoResponsavel
) {
    public static final int MAX_TITULO_BYTES = 100;
    public static final int MAX_DESCRICAO_BYTES = 1000;

    public TriagemIaResult comLimitesOracle() {
        return new TriagemIaResult(
                ocorrenciaValida,
                truncarUtf8(titulo, MAX_TITULO_BYTES),
                truncarUtf8(descricao, MAX_DESCRICAO_BYTES),
                categoriaSugerida,
                prioridade,
                orgaoResponsavel
        );
    }

    public static String truncarUtf8(String texto, int maxBytes) {
        String valor = texto.trim();
        if (valor.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return valor;
        }
        int bytes = 0;
        int endIndex = 0;
        while (endIndex < valor.length()) {
            int cp = valor.codePointAt(endIndex);
            int cpBytes = cp <= 0x7F ? 1 : cp <= 0x7FF ? 2 : cp <= 0xFFFF ? 3 : 4;
            if (bytes + cpBytes > maxBytes) break;
            bytes += cpBytes;
            endIndex += Character.charCount(cp);
        }
        return valor.substring(0, endIndex).trim();
    }
}
