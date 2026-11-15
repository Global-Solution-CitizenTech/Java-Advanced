package fiap.com.br.zelafix.historico.dto;

import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.validation.EnumValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HistoricoUpdateRequest(
        @NotBlank(message = "O status é obrigatório.")
        @EnumValidation(enumClass = StatusChamadoEnum.class, message = "Status inválido. Valores aceitos: PENDENTE, EM_ANALISE, EM_EXECUCAO, CONCLUIDO, REJEITADO")
        String status,

        @NotBlank(message = "A observação é obrigatória.")
        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
        String observacao
) {}
