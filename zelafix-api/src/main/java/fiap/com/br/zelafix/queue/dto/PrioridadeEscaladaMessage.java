package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeEnum;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.status.Status;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PrioridadeEscaladaMessage(
        Long idChamado,
        PrioridadeEnum novaPrioridade,
        int totalVotos,
        String motivo
) {
    public HistChamado toHistorico(Chamado chamado, Status status, PrioridadeEnum anterior) {
        return HistChamado.builder()
                .chamado(chamado)
                .usuario(chamado.getUsuario())
                .status(status)
                .observacoes("Prioridade escalada de %s para %s após atingir %d apoios comunitários.".formatted(anterior, novaPrioridade, totalVotos))
                .build();
    }
}
