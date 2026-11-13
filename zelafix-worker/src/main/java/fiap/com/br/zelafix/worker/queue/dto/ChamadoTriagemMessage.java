package fiap.com.br.zelafix.worker.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long idChamado,
        String bairro,
        byte[] foto
) {
}
