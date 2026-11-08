package fiap.com.br.zelafix.chamado.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.UrgenciaChamado;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

public record ChamadoUpdateRequest(
        @Size(max = 100, message = "O título pode ter no máximo 100 caracteres")
        String titulo,

        @Size(max = 1000, message = "A descrição pode ter no máximo 1000 caracteres")
        String descricao,

        String categoria,

        String prioridade,

        @Valid
        EnderecoRequest endereco,

        String fotoUrl,

        String status,

        String feedbackResolucao
) {
    public void aplicarEm(Chamado chamado, Categoria categoria, Endereco endereco) {
        if (titulo != null && !titulo.isBlank()) {
            chamado.setTitulo(titulo.trim());
        }
        if (descricao != null && !descricao.isBlank()) {
            chamado.setDescricao(descricao.trim());
        }
        if (categoria != null) {
            chamado.setCategoria(categoria);
        }
        if (prioridade != null && !prioridade.isBlank()) {
            chamado.setPrioridade(UrgenciaChamado.fromString(prioridade));
        }
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            chamado.setFotoUrl(fotoUrl.trim());
        }
        if (feedbackResolucao != null) {
            chamado.setFeedbackResolucao(feedbackResolucao.trim());
        }
        if (endereco != null) {
            chamado.setEndereco(endereco);
        }
    }
}
