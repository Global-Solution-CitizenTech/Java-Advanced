package fiap.com.br.zelafix.worker.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VotoMessage(
        Long idChamado,
        Long idUsuario,
        String operacao
) {

    public VotoMessage(Long idChamado, Long idUsuario) {
        this(idChamado, idUsuario, "VOTAR");
    }
}
