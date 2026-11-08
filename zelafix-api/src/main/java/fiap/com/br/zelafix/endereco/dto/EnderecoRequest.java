package fiap.com.br.zelafix.endereco.dto;

import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve estar no formato 00000-000 ou 00000000")
        String cep,

        @NotBlank(message = "O número é obrigatório")
        @Size(max = 100, message = "Número deve ter no máximo 100 caracteres")
        String numero,

        String complemento
) {
    public Endereco toEntity(String rua, Bairro bairro) {
        return Endereco.builder()
                .cep(cep.replaceAll("\\D", ""))
                .numero(numero)
                .rua(rua)
                .complemento(complemento)
                .bairro(bairro)
                .build();
    }
}
