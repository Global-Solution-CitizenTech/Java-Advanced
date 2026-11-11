package fiap.com.br.zelafix.worker.queue.dto;

public record ChamadoTriagemMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoria,
        String bairro
) {}
