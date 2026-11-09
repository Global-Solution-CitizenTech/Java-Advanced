package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.status.Status;
import fiap.com.br.zelafix.status.StatusChamadoEnum;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.validation.EnumValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record StatusRequest(
        @NotBlank(message = "O status é obrigatório.")
        @EnumValidation(enumClass = StatusChamadoEnum.class, message = "Status inválido. Valores aceitos: PENDENTE, EM_ANALISE, EM_EXECUCAO, CONCLUIDO, REJEITADO, ABERTO")
        String status,

        @NotBlank(message = "A observação de auditoria é obrigatória.")
        @Size(max = 255, message = "A observação deve ter no máximo 255 caracteres.")
        String observacao
) {
    public HistChamado toEntity(Chamado chamado, Usuario operador, Status novoStatus) {
        return HistChamado.builder()
                .chamado(chamado)
                .usuario(operador)
                .status(novoStatus)
                .dtMudanca(LocalDateTime.now())
                .observacoes(observacao)
                .build();
    }
}
