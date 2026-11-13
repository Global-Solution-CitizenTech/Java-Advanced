package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.chamado.Chamado;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long idChamado,
        String bairro,
        byte[] foto
) {
    public static ChamadoTriagemMessage from(Chamado chamado) {
        return new ChamadoTriagemMessage(
                chamado.getIdChamado(),
                chamado.getEndereco().getBairro().getNome(),
                chamado.getFoto()
        );
    }
}
