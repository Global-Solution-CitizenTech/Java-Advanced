package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChamadoCreateRequest(
        @NotNull(message = "O endereço da ocorrência é obrigatório.")
        @Valid
        EnderecoRequest endereco,

        @NotBlank(message = "A foto da ocorrência é obrigatória.")
        String fotoBase64
) {}
