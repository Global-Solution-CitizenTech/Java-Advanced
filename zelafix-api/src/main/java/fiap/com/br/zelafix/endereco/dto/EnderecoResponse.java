package fiap.com.br.zelafix.endereco.dto;

import fiap.com.br.zelafix.endereco.Endereco;

public record EnderecoResponse(
        Long id,
        String cep,
        String numero,
        String rua,
        String referencia,
        String complemento,
        String bairro,
        String cidade,
        String estado
) {
    public static EnderecoResponse fromEntity(Endereco endereco) {
        if (endereco == null) return null;

        String bairro = (endereco.getBairro() != null) ? endereco.getBairro().getNome() : null;
        String cidade = (endereco.getBairro() != null && endereco.getBairro().getCidade() != null)
                ? endereco.getBairro().getCidade().getNome()
                : null;
        String estado = (endereco.getBairro() != null && endereco.getBairro().getCidade() != null && endereco.getBairro().getCidade().getEstado() != null)
                ? endereco.getBairro().getCidade().getEstado().getNome()
                : null;

        return new EnderecoResponse(
                endereco.getId(),
                endereco.getCep(),
                endereco.getNumero(),
                endereco.getRua(),
                endereco.getReferencia(),
                endereco.getReferencia(),
                bairro,
                cidade,
                estado
        );
    }
}
