package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VotoMessage(
        Long idChamado,
        Long idUsuario,
        String operacao
) implements Serializable {

    public VotoMessage(Long idChamado, Long idUsuario) {
        this(idChamado, idUsuario, "VOTAR");
    }
}
