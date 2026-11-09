package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;

import java.util.Collections;
import java.util.List;

public record ChamadoResponse(
        String id,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        String status,
        String prioridade,
        Integer apoiosCount,
        boolean apoiadoPeloUsuario,
        String parecerIa,
        String orgaoResponsavel,
        EnderecoResponse endereco,
        String fotoUrl,
        String dataAbertura,
        String dataAtualizacao,
        String autorId,
        String autorNome,
        String feedbackResolucao,
        List<HistoricoResponse> historico
) {
    public static ChamadoResponse fromEntity(Chamado c, Long currentUserId) {
        if (c == null) return null;

        boolean apoiado = false;
        if (currentUserId != null && c.getVotos() != null) {
            apoiado = c.getVotos().stream()
                    .anyMatch(v -> v.getUsuario() != null && currentUserId.equals(v.getUsuario().getIdUsuario()));
        }

        List<HistoricoResponse> hist = (c.getHistorico() != null)
                ? c.getHistorico().stream().map(HistoricoResponse::fromEntity).toList()
                : Collections.emptyList();

        String statusNome = (c.getStatus() != null) ? c.getStatus().getNmStatus() : "ABERTO";
        String catNome = (c.getCategoria() != null) ? c.getCategoria().getNmCategoria() : "Geral";
        String prioDesc = (c.getPrioridade() != null) ? c.getPrioridade().getDescricao() : "Média";
        String fotoRota = c.getIdChamado() != null ? "/chamados/" + c.getIdChamado() + "/foto" : "/chamados/foto";

        return new ChamadoResponse(
                String.valueOf(c.getIdChamado()),
                c.getProtocolo(),
                c.getTitulo(),
                c.getDescricao(),
                catNome,
                statusNome,
                prioDesc,
                c.getApoiosCount(),
                apoiado,
                c.getParecerIa(),
                c.getOrgaoResponsavel(),
                EnderecoResponse.fromEntity(c.getEndereco()),
                fotoRota,
                c.getDtAbertura() != null ? c.getDtAbertura().toString() : null,
                c.getDtConclusao() != null ? c.getDtConclusao().toString() : null,
                c.getUsuario() != null ? String.valueOf(c.getUsuario().getIdUsuario()) : null,
                c.getUsuario() != null ? c.getUsuario().getNmUsuario() : null,
                c.getFeedbackResolucao(),
                hist
        );
    }
}
