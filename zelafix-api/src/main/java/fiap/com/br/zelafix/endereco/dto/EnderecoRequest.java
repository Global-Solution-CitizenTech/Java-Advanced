package fiap.com.br.zelafix.endereco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank String cep,
        @NotBlank String rua,
        @NotBlank String numero,
        @NotBlank @Size(max = 50) String referencia
) {}
