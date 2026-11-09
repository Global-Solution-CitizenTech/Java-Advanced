package fiap.com.br.zelafix.status.dto;

import fiap.com.br.zelafix.status.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusRequest(
        @NotBlank(message = "O nome do status é obrigatório.")
        @Size(max = 20, message = "O nome do status deve ter no máximo 20 caracteres.")
        String nome
) {
    public Status toEntity() {
        return Status.builder()
                .nmStatus(nome.trim().toUpperCase())
                .build();
    }
}
