package fiap.com.br.zelafix.usuario.dto;

import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.validation.EnumValidation;
import jakarta.validation.constraints.NotBlank;

public record CargoUpdateRequest(
        @NotBlank(message = "O cargo é obrigatório.")
        @EnumValidation(enumClass = CargoUsuario.class, message = "Cargo inválido. Valores aceitos: CIDADAO, GESTOR, ADMIN")
        String cargo
) {}
