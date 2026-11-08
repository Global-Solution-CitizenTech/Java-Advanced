package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long idChamado,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        String bairro
) implements Serializable {

    public Long chamadoId() {
        return this.idChamado;
    }
}
