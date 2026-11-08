package fiap.com.br.zelafix.categoria.dto;

import fiap.com.br.zelafix.categoria.Categoria;

public record CategoriaResponse(
        Long id,
        String nome
) {
    public static CategoriaResponse fromEntity(Categoria c) {
        return new CategoriaResponse(c.getIdCategoria(), c.getNmCategoria());
    }
}
