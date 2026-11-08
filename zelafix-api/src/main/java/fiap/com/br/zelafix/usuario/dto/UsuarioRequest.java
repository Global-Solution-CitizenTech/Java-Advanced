package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;

public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        CargoUsuario cargo,

        String cpf,

        String telefone,

        @NotNull(message = "O endereço é obrigatório")
        @Valid
        EnderecoRequest endereco
) {
    public Usuario toEntity(Endereco endereco, String senhaCodificada) {
        return Usuario.builder()
                .nmUsuario(nome.trim())
                .email(email.trim().toLowerCase())
                .senha(senhaCodificada)
                .cargo(cargo != null ? cargo : CargoUsuario.CIDADAO)
                .cpf(cpf)
                .endereco(endereco)
                .telefones(new ArrayList<>())
                .build();
    }

    public void aplicarEm(Usuario usuario, Endereco endereco, PasswordEncoder passwordEncoder) {
        if (nome != null && !nome.isBlank()) {
            usuario.setNmUsuario(nome.trim());
        }
        if (cpf != null && !cpf.isBlank()) {
            usuario.setCpf(cpf.trim());
        }
        if (senha != null && !senha.isBlank() && passwordEncoder != null) {
            usuario.setSenha(passwordEncoder.encode(senha));
        }
        if (endereco != null) {
            usuario.setEndereco(endereco);
        }
    }

    public Telefone toTelefone(Usuario usuario) {
        if (telefone == null || telefone.isBlank()) return null;
        String digits = telefone.replaceAll("\\D", "");
        if (digits.length() < 10) return null;
        return Telefone.builder()
                .numDdd(digits.substring(0, 2))
                .numTel(digits.substring(2, Math.min(digits.length(), 11)))
                .usuario(usuario)
                .build();
    }
}
