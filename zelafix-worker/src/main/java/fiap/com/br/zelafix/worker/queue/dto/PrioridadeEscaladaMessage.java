package fiap.com.br.zelafix.worker.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrioridadeEscaladaMessage(
        Long idChamado,
        PrioridadeChamado novaPrioridade,
        int totalVotos,
        String motivo
) {
}
