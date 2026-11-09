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

        @Size(max = 50, message = "Referência deve ter no máximo 50 caracteres")
        String referencia,

        String complemento
) {
    public EnderecoRequest(String cep, String numero, String referencia) {
        this(cep, numero, referencia, referencia);
    }

    public Endereco toEntity(String rua, Bairro bairro) {
        String ref = (referencia != null && !referencia.isBlank()) ? referencia : complemento;
        if (ref == null || ref.isBlank()) {
            ref = "Sem referência";
        }
        if (ref.length() > 50) {
            ref = ref.substring(0, 50);
        }

        return Endereco.builder()
                .cep(cep.replaceAll("\\D", ""))
                .numero(numero)
                .rua(rua)
                .referencia(ref)
                .bairro(bairro)
                .build();
    }
}
