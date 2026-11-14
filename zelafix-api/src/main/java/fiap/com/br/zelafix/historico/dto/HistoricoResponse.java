package fiap.com.br.zelafix.historico.dto;

import fiap.com.br.zelafix.historico.HistChamado;

public record HistoricoResponse(
        Long id,
        String data,
        String status,
        String observacao,
        String autorNome
) {
    public static HistoricoResponse fromEntity(HistChamado h) {
        return new HistoricoResponse(
                h.getIdHist(),
                h.getDtMudanca().toString(),
                h.getStatus().getNmStatus().name(),
                h.getObservacoes(),
                h.getUsuario().getNmUsuario()
        );
    }
}
