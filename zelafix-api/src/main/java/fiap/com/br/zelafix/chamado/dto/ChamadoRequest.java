package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
        @NotBlank @Size(max = 100) String titulo,
        @NotBlank @Size(max = 1000) String descricao,
        @NotBlank String categoria,
        @NotNull @Valid EnderecoRequest endereco,
        String fotoBase64
) {}

