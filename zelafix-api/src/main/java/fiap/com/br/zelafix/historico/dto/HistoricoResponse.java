package fiap.com.br.zelafix.historico.dto;

import fiap.com.br.zelafix.historico.HistChamado;

public record HistoricoResponse(
        Long id,
        String data,
        String status,
        String observacao,
        String autorNome
) {
    public HistoricoResponse(HistChamado h) {
        this(
                h.getIdHist(),
                h.getDtMudanca().toString(),
                h.getStatus().getNmStatus(),
                h.getObservacoes(),
                h.getUsuario().getNmUsuario()
        );
    }
}
