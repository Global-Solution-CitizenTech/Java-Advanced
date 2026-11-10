package fiap.com.br.zelafix.endereco.dto;

import fiap.com.br.zelafix.endereco.Endereco;

public record EnderecoResponse(
        Long id,
        String cep,
        String numero,
        String rua,
        String referencia,
        String bairro,
        String cidade,
        String estado
) {
    public static EnderecoResponse fromEntity(Endereco endereco) {
        return new EnderecoResponse(
                endereco.getId(),
                endereco.getCep(),
                endereco.getNumero(),
                endereco.getRua(),
                endereco.getReferencia(),
                endereco.getBairro().getNome(),
                endereco.getBairro().getCidade().getNome(),
                endereco.getBairro().getCidade().getEstado().getNome()
        );
    }
}
