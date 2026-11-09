package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record StatusUpdateRequest(
        @NotBlank(message = "O novo status é obrigatório")
        String novoStatus,

        String observacoes,
        String feedbackResolucao
) {
    public HistChamado toHistorico(Chamado chamado, Usuario usuario, Status status, String statusAnterior) {
        String detalhe = (observacoes != null && !observacoes.isBlank()) ? " (" + observacoes.trim() + ")" : "";
        String obs = "Status alterado de [" + statusAnterior + "] para [" + status.getNmStatus() + "]" + detalhe;
        if (obs.length() > 255) {
            obs = obs.substring(0, 255);
        }

        return HistChamado.builder()
                .chamado(chamado)
                .usuario(usuario)
                .status(status)
                .observacoes(obs)
                .dtMudanca(LocalDateTime.now())
                .build();
    }
}
