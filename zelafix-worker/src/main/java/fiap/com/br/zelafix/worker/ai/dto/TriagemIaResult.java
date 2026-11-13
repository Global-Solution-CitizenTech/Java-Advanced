package fiap.com.br.zelafix.worker.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;

public record TriagemIaResult(
        @JsonPropertyDescription("Categoria oficial do problema urbano observado na foto")
        CategoriaSugerida categoriaSugerida,

        @JsonPropertyDescription("Nível de prioridade da ocorrência urbana")
        PrioridadeChamado prioridade,

        @JsonPropertyDescription("Órgão ou secretaria municipal competente para a resolução")
        DepartamentoGoverno orgaoResponsavel,

        @JsonPropertyDescription("Parecer técnico sucinto (máximo 150 caracteres) justificando a classificação e o risco observado")
        String parecerIa
) {}
