package fiap.com.br.zelafix.queue.dto;

import java.io.Serializable;

public record TriagemFeedbackMessage(
        Long idChamado,
        String prioridade,
        String orgao,
        String parecer
) implements Serializable {}
