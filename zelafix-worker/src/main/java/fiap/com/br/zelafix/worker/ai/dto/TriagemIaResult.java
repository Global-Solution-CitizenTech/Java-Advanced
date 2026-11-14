package fiap.com.br.zelafix.worker.ai.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;

public record TriagemIaResult(
        @JsonPropertyDescription("true somente se a foto mostrar uma ocorrência real de zeladoria em espaço público urbano (rua, calçada, praça, poste, bueiro, semáforo); false se for captura de tela, jogo, anime, ilustração, meme, selfie, pessoa, animal, objeto ou ambiente interno")
        boolean ocorrenciaValida,

        @JsonPropertyDescription("Título curto, direto e em português do Brasil natural com capitalização normal de frase, sem usar tudo em maiúsculas (máximo 70 caracteres para respeitar o limite de bytes UTF-8 do Oracle)")
        String titulo,

        @JsonPropertyDescription("Descrição objetiva em português do Brasil correto explicando o problema visível na foto, seu impacto na via/calçada e o reparo necessário (máximo 500 caracteres para respeitar o limite de bytes UTF-8 do Oracle)")
        String descricao,

        @JsonPropertyDescription("Categoria oficial do problema urbano observado na foto")
        CategoriaSugerida categoriaSugerida,

        @JsonPropertyDescription("Nível de prioridade da ocorrência urbana")
        PrioridadeChamado prioridade,

        @JsonPropertyDescription("Órgão ou secretaria municipal competente para a resolução")
        DepartamentoGoverno orgaoResponsavel
) {}
