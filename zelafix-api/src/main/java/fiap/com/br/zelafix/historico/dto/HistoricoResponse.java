package fiap.com.br.zelafix.historico.dto;

import fiap.com.br.zelafix.historico.HistStatus;

public record HistoricoResponse(
        String id,
        String data,
        String status,
        String observacao,
        String autorNome
) {
    public static HistoricoResponse fromEntity(HistStatus h) {
        return new HistoricoResponse(
                String.valueOf(h.getIdHist()),
                h.getDtMudanca().toString(),
                h.getStatus().getNmStatus(),
                h.getObservacoes(),
                h.getUsuario().getNmUsuario()
        );
    }
}
