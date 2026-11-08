package fiap.com.br.zelafix.endereco.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ViaCepResponse(
        String logradouro,
        String bairro,
        String localidade,
        String estado,
        Boolean erro
) {
}
