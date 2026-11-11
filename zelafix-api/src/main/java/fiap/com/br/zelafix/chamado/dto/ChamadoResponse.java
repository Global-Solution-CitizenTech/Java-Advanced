package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;

import java.time.LocalDateTime;
import java.util.List;

public record ChamadoResponse(
        Long idChamado,
        String titulo,
        String descricao,
        LocalDateTime dtAbertura,
        Long categoriaId,
        String categoriaNome,
        Long usuarioId,
        String usuarioNome,
        EnderecoResponse endereco,
        String foto,
        String status,
        long totalVotos,
        boolean apoiadoPorMim,
        List<HistoricoResponse> historico
) {
    public static ChamadoResponse fromEntity(Chamado c, String status, long totalVotos, boolean apoiado, List<HistChamado> historicos) {
        return new ChamadoResponse(
                c.getIdChamado(),
                c.getTitulo(),
                c.getDescricao(),
                c.getDtAbertura(),
                c.getCategoria().getIdCategoria(),
                c.getCategoria().getNmCategoria(),
                c.getUsuario().getIdUsuario(),
                c.getUsuario().getNmUsuario(),
                EnderecoResponse.fromEntity(c.getEndereco()),
                "/chamados/" + c.getIdChamado() + "/foto",
                status,
                totalVotos,
                apoiado,
                historicos.stream().map(HistoricoResponse::fromEntity).toList()
        );
    }
}
