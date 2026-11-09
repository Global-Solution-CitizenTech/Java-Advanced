package fiap.com.br.zelafix.chamado.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusRequest(
        @NotBlank String status,
        @Size(max = 255) String observacao
) {}

