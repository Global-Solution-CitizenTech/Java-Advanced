package fiap.com.br.zelafix.worker.queue.dto;

public record TriagemFeedbackMessage(
        Long idChamado,
        String prioridade,
        String orgao,
        String parecer
) {}
