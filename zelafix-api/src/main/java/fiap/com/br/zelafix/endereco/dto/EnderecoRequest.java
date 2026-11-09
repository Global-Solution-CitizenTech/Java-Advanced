package fiap.com.br.zelafix.endereco.dto;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "O CEP é obrigatório.")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve estar no formato 00000-000 ou 00000000.")
        String cep,

        @NotBlank(message = "O número é obrigatório.")
        @Size(max = 100, message = "Número deve ter no máximo 100 caracteres.")
        String numero,

        @NotBlank(message = "A referência é obrigatória conforme modelagem.")
        @Size(max = 50, message = "Referência deve ter no máximo 50 caracteres.")
        String referencia,

        @Size(max = 100, message = "Logradouro/Rua deve ter no máximo 100 caracteres.")
        String rua
) {
    public Endereco toEntity(String ruaResolvida, Bairro bairro) {
        String logradouro = (rua != null && !rua.isBlank()) ? rua.trim() : ruaResolvida;
        return Endereco.builder()
                .cep(cep.replaceAll("\\D", ""))
                .numero(numero.trim())
                .referencia(referencia.trim())
                .rua(logradouro)
                .bairro(bairro)
                .build();
    }
}
