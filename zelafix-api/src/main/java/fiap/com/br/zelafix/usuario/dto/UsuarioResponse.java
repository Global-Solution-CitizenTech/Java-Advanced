package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.usuario.Usuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String cargo,
        EnderecoResponse endereco
) {
    public UsuarioResponse(Usuario u) {
        this(
                u.getIdUsuario(),
                u.getNmUsuario(),
                u.getEmail(),
                u.getCargo().name(),
                new EnderecoResponse(u.getEndereco())
        );
    }
}
