package fiap.com.br.zelafix.queue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fiap.com.br.zelafix.categoria.Categoria;
import fiap.com.br.zelafix.categoria.CategoriaEnum;
import fiap.com.br.zelafix.chamado.Chamado;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGoverno;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGovernoEnum;
import fiap.com.br.zelafix.chamado.prioridade.Prioridade;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeEnum;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.historico.HistChamado;
import fiap.com.br.zelafix.historico.status.Status;
import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.usuario.Usuario;
import lombok.Builder;

import java.util.List;
import java.util.function.Function;


@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TriagemFeedbackMessage(
        String correlationId,
        Long usuarioId,
        Long enderecoId,
        byte[] foto,
        String titulo,
        String descricao,
        CategoriaEnum categoria,
        PrioridadeEnum prioridade,
        OrgaoGovernoEnum orgao,
        StatusChamadoEnum status,
        String observacoes,
        boolean recusado,
        String erro
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
                .observacoes(observacoes)
                .build();
    }

    public List<HistChamado> toHistoricos(Chamado chamado, Function<StatusChamadoEnum, Status> statusLookup) {
        HistChamado inicial = HistChamado.builder()
                .chamado(chamado)
                .usuario(chamado.getUsuario())
                .status(statusLookup.apply(StatusChamadoEnum.PENDENTE))
                .observacoes("Chamado registrado pelo cidadão " + chamado.getUsuario().getNmUsuario() + " via ZelaFix Mobile.")
                .build();
        return status == StatusChamadoEnum.PENDENTE
                ? List.of(inicial)
                : List.of(inicial, toEntity(chamado, statusLookup.apply(status)));
    }
}
