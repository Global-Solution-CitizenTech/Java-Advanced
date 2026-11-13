package fiap.com.br.zelafix.queue.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;

public record TriagemFeedbackMessage(
        Long idChamado,
        String titulo,
        String descricao,
        String categoriaNome,
        String prioridadeNome,
        String orgaoNome
) {
    public Chamado toEntity(Chamado base, Categoria categoria, Prioridade prioridade, OrgaoGoverno orgao) {
        return Chamado.builder()
                .idChamado(base.getIdChamado())
                .titulo(titulo)
                .descricao(descricao)
                .foto(base.getFoto())
                .dtAbertura(base.getDtAbertura())
                .usuario(base.getUsuario())
                .endereco(base.getEndereco())
                .categoria(categoria)
                .prioridade(prioridade)
                .orgaoGoverno(orgao)
                .build();
    }
}
