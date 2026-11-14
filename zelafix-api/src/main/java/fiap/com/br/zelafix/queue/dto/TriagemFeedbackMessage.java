package fiap.com.br.zelafix.queue.dto;

import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;

import java.time.LocalDateTime;

public record TriagemFeedbackMessage(
        Long usuarioId,
        Long enderecoId,
        byte[] foto,
        String titulo,
        String descricao,
        String categoriaNome,
        String prioridadeNome,
        String orgaoNome,
        String statusNome,
        boolean recusado
) {
    public Chamado toEntity(
            Usuario usuario,
            Endereco endereco,
            Categoria categoria,
            Prioridade prioridade,
            OrgaoGoverno orgaoGoverno
    ) {
        return Chamado.builder()
                .titulo(titulo)
                .descricao(descricao)
                .foto(foto)
                .dtAbertura(LocalDateTime.now())
                .usuario(usuario)
                .endereco(endereco)
                .categoria(categoria)
                .prioridade(prioridade)
                .orgaoGoverno(orgaoGoverno)
                .build();
    }

    public HistChamado toEntity(Chamado chamado, Status status) {
        return HistChamado.builder()
                .chamado(chamado)
                .usuario(chamado.getUsuario())
                .status(status)
                .dtMudanca(LocalDateTime.now())
                .observacoes("Triagem concluída. Prioridade: %s. Categoria: %s. Órgão: %s.".formatted(prioridadeNome(), categoriaNome(), orgaoNome()))
                .build();
    }
}
