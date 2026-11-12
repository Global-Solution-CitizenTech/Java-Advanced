package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.telefone.Telefone;
import fiap.com.br.zelafix.validation.DddValidation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, max = 60, message = "Senha deve ter entre 6 e 60 caracteres.")
        String senha,

        @NotBlank(message = "O DDD é obrigatório.")
        @Pattern(regexp = "^\\d{2}$", message = "DDD deve conter exatamente 2 dígitos numéricos.")
        @DddValidation
        String ddd,

        @NotBlank(message = "O número de telefone é obrigatório.")
        @Pattern(regexp = "^\\d{8,9}$", message = "Telefone deve conter 8 ou 9 dígitos numéricos.")
        String numeroTelefone,

        @NotNull(message = "O endereço é obrigatório.")
        @Valid
        EnderecoRequest endereco
) {
    public Usuario toEntity(Endereco endereco, String senhaCodificada) {
        Usuario usuario = Usuario.builder()
                .nmUsuario(nome.trim())
                .email(email.trim().toLowerCase())
                .senha(senhaCodificada)
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endereco)
                .build();

        usuario.setTelefone(toTelefone(usuario));
        return usuario;
    }

    public Telefone toTelefone(Usuario usuario) {
        return Telefone.builder()
                .numDdd(ddd.trim())
                .numTel(numeroTelefone.trim())
                .usuario(usuario)
                .build();
    }
}
