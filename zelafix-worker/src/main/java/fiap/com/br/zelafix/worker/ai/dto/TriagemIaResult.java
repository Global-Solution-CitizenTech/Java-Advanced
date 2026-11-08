package fiap.com.br.zelafix.worker.ai.dto;

import java.io.Serializable;

public record TriagemIaResult(
        String prioridade,
        String orgaoResponsavel,
        String parecerIa
) implements Serializable {}

