package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.usuario.Usuario;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        String correlationId,
        Long usuarioId,
        Long enderecoId,
        String bairro,
        byte[] foto
) {
    public static ChamadoTriagemMessage from(String correlationId, Usuario autor, Endereco endereco, byte[] foto) {
        return new ChamadoTriagemMessage(
                correlationId,
                autor.getIdUsuario(),
                endereco.getId(),
                endereco.getBairro().getNome(),
                foto
        );
    }
}
