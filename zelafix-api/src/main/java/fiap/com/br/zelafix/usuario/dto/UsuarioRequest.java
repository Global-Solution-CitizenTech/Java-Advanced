package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioRequest(
        @NotBlank @Size(min = 3, max = 100) String nome,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String senha,
        CargoUsuario cargo,
        @NotNull @Valid EnderecoRequest endereco
) {}
