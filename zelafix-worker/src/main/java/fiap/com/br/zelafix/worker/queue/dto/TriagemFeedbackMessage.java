package fiap.com.br.zelafix.worker.queue.dto;

public record TriagemFeedbackMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoriaNome,
        String prioridadeNome,
        String orgaoNome
) {}
