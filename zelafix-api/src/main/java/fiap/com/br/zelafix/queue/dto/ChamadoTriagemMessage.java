package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.usuario.Usuario;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChamadoTriagemMessage(
        Long usuarioId,
        Long enderecoId,
        String bairro,
        byte[] foto
) {
    public static ChamadoTriagemMessage from(Usuario autor, Endereco endereco, byte[] foto) {
        return new ChamadoTriagemMessage(
                autor.getIdUsuario(),
                endereco.getId(),
                endereco.getBairro().getNome(),
                foto
        );
    }
}
