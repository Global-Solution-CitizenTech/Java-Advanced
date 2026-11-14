package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.usuario.Usuario;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TriagemFeedbackMessage(
        String correlationId,
        Long usuarioId,
        Long enderecoId,
        byte[] foto,
        String titulo,
        String descricao,
        String categoriaNome,
        String prioridadeNome,
        String orgaoNome,
        String statusNome,
        boolean recusado,
        String erro
) {
    public TriagemFeedbackMessage(
            String correlationId,
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
        this(correlationId, usuarioId, enderecoId, foto, titulo, descricao,
                categoriaNome, prioridadeNome, orgaoNome, statusNome, recusado, null);
    }
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
