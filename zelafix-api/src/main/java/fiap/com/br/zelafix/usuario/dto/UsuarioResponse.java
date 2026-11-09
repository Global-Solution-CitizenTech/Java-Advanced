package fiap.com.br.zelafix.usuario.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.usuario.Usuario;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String cargo,
        String telefone,
        EnderecoResponse endereco,
        String cpf,
        String avatarUrl,
        String createdAt
) {
    public UsuarioResponse(String id, String nome, String email, String cargo, String telefone, EnderecoResponse endereco) {
        this(id, nome, email, cargo, telefone, endereco, null, null, null);
    }

    public static UsuarioResponse fromEntity(Usuario u) {
        if (u == null) return null;
        String tel = (u.getTelefones() != null && !u.getTelefones().isEmpty())
                ? "(" + u.getTelefones().get(0).getNumDdd() + ") " + u.getTelefones().get(0).getNumTel()
                : null;

        return new UsuarioResponse(
                String.valueOf(u.getIdUsuario()),
                u.getNmUsuario(),
                u.getEmail(),
                u.getCargo() != null ? u.getCargo().name() : "CIDADAO",
                tel,
                EnderecoResponse.fromEntity(u.getEndereco()),
                null,
                null,
                null
        );
    }
}
