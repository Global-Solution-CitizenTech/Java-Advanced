package fiap.com.br.zelafix.chamado.dto;

import jakarta.validation.constraints.NotBlank;

public record ChamadoTriagemRequest(
        String titulo,
        String descricao,
        String bairro,
        @NotBlank(message = "A foto em Base64 é obrigatória para análise pela IA.")
        String fotoBase64
) {}
