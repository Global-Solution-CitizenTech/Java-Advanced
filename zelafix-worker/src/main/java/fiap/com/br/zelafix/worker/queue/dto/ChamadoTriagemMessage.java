package fiap.com.br.zelafix.worker.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoria,
        String bairro
) implements Serializable {}
