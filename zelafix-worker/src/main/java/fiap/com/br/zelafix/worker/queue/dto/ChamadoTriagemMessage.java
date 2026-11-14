package fiap.com.br.zelafix.worker.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        String correlationId,
        Long usuarioId,
        Long enderecoId,
        String bairro,
        byte[] foto
) {
}
