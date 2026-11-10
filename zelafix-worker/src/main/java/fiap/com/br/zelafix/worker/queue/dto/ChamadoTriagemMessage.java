package fiap.com.br.zelafix.worker.queue.dto;

import java.io.Serializable;

public record ChamadoTriagemMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoria,
        String bairro
) implements Serializable {}
