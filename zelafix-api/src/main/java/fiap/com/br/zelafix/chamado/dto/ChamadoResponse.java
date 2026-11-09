package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime dataAbertura,
        String categoria,
        String status,
        String autor,
        Long autorId,
        EnderecoResponse endereco,
        String fotoUrl,
        long apoios,
        boolean apoiadoPorMim,
        List<HistoricoResponse> historico
) {
    public ChamadoResponse(Chamado c, String status, long apoios, boolean apoiadoPorMim, List<HistoricoResponse> historico) {
        this(
                c.getIdChamado(),
                c.getTitulo(),
                c.getDescricao(),
                c.getDtAbertura(),
                c.getCategoria().getNmCategoria(),
                status,
                c.getUsuario().getNmUsuario(),
                c.getUsuario().getIdUsuario(),
                new EnderecoResponse(c.getEndereco()),
                "/chamados/" + c.getIdChamado() + "/foto",
                apoios,
                apoiadoPorMim,
                historico
        );
    }
}
