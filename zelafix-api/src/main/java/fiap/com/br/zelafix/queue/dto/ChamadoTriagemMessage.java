package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.chamado.Chamado;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoria,
        String bairro
) {

    public static ChamadoTriagemMessage from(Chamado chamado) {
        return new ChamadoTriagemMessage(
                chamado.getIdChamado(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getCategoria().getNmCategoria(),
                chamado.getEndereco().getBairro().getNome()
        );
    }
}
