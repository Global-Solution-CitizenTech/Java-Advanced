package fiap.com.br.zelafix.endereco.dto;

import fiap.com.br.zelafix.endereco.Endereco;

public record EnderecoResponse(
        Long id,
        String cep,
        String rua,
        String numero,
        String referencia,
        String bairro,
        String cidade,
        String estado
) {
    public EnderecoResponse(Endereco e) {
        this(
                e.getId(),
                e.getCep(),
                e.getRua(),
                e.getNumero(),
                e.getReferencia(),
                e.getBairro().getNome(),
                e.getBairro().getCidade().getNome(),
                e.getBairro().getCidade().getEstado().getNome()
        );
    }
}
