package fiap.com.br.zelafix.historico.dto;

import fiap.com.br.zelafix.historico.HistChamado;

public record HistoricoResponse(
        String id,
        String data,
        String status,
        String observacao,
        String autorNome
) {
    public static HistoricoResponse fromEntity(HistChamado h) {
        return new HistoricoResponse(
                String.valueOf(h.getIdHist()),
                h.getDtMudanca() != null ? h.getDtMudanca().toString() : null,
                h.getStatus() != null ? h.getStatus().getNmStatus() : "ABERTO",
                h.getObservacoes(),
                h.getUsuario() != null ? h.getUsuario().getNmUsuario() : "Sistema"
        );
    }
}
