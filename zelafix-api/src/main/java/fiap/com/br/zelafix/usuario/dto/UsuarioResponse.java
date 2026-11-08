package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.usuario.Usuario;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String cargo,
        String cpf,
        String telefone,
        EnderecoResponse endereco,
        String avatarUrl,
        String createdAt
) {
    public static UsuarioResponse fromEntity(Usuario u) {
        String tel = (u.getTelefones() != null && !u.getTelefones().isEmpty())
                ? "(" + u.getTelefones().get(0).getNumDdd() + ") " + u.getTelefones().get(0).getNumTel()
                : null;

        return new UsuarioResponse(
                String.valueOf(u.getIdUsuario()),
                u.getNmUsuario(),
                u.getEmail(),
                u.getCargo().name(),
                u.getCpf(),
                tel,
                EnderecoResponse.fromEntity(u.getEndereco()),
                u.getAvatarUrl(),
                u.getDtCadastro() != null ? u.getDtCadastro().toString() : null
        );
    }
}
