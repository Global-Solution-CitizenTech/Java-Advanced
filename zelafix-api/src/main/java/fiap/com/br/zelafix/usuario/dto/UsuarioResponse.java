package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.usuario.Usuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String cargo,
        String ddd,
        String numeroTelefone,
        EnderecoResponse endereco
) {
    public static UsuarioResponse fromEntity(Usuario usuario) {
        var telefone = (usuario.getTelefones() != null && !usuario.getTelefones().isEmpty())
                ? usuario.getTelefones().getFirst()
                : null;
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNmUsuario(),
                usuario.getEmail(),
                usuario.getCargo().name(),
                telefone != null ? telefone.getNumDdd() : null,
                telefone != null ? telefone.getNumTel() : null,
                EnderecoResponse.fromEntity(usuario.getEndereco())
        );
    }
}
