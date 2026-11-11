package fiap.com.br.zelafix.queue.dto;

import fiap.com.br.zelafix.historico.HistChamado;

import java.time.LocalDateTime;

public record TriagemFeedbackMessage(
        Long idChamado,
        String prioridade,
        String orgao,
        String parecer
) {

    public HistChamado toEntity(HistChamado base) {
        String obs = "[Triagem IA - %s | %s] %s".formatted(prioridade, orgao, parecer);
        return HistChamado.builder()
                .chamado(base.getChamado())
                .usuario(base.getUsuario())
                .status(base.getStatus())
                .dtMudanca(LocalDateTime.now())
                .observacoes(obs.length() > 255 ? obs.substring(0, 252) + "..." : obs)
                .build();
    }
}
