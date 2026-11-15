package fiap.com.br.zelafix.worker.queue.dto;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.ai.enums.StatusChamado;
import lombok.Builder;

@Builder
public record TriagemFeedbackMessage(
        String correlationId,
        Long usuarioId,
        Long enderecoId,
        byte[] foto,
        String titulo,
        String descricao,
        CategoriaSugerida categoria,
        PrioridadeChamado prioridade,
        DepartamentoGoverno orgao,
        StatusChamado status,
        String observacoes,
        boolean recusado,
        String erro
) {
    public static TriagemFeedbackMessage sucesso(ChamadoTriagemMessage origem, TriagemIaResult resultado) {
        String obs = "Triagem automática por IA concluída. Prioridade: %s. Categoria: %s. Órgão: %s."
                .formatted(resultado.prioridade(), resultado.categoriaSugerida(), resultado.orgaoResponsavel());
        return TriagemFeedbackMessage.builder()
                .correlationId(origem.correlationId())
                .usuarioId(origem.usuarioId())
                .enderecoId(origem.enderecoId())
                .foto(origem.foto())
                .titulo(resultado.titulo())
                .descricao(resultado.descricao())
                .categoria(resultado.categoriaSugerida())
                .prioridade(resultado.prioridade())
                .orgao(resultado.orgaoResponsavel())
                .status(StatusChamado.EM_ANALISE)
                .observacoes(obs)
                .build();
    }

    public static TriagemFeedbackMessage recusa(ChamadoTriagemMessage origem, TriagemIaResult resultado) {
        return TriagemFeedbackMessage.builder()
                .correlationId(origem.correlationId())
                .usuarioId(origem.usuarioId())
                .enderecoId(origem.enderecoId())
                .titulo(resultado.titulo())
                .descricao(resultado.descricao())
                .recusado(true)
                .build();
    }

    public static TriagemFeedbackMessage contingencia(ChamadoTriagemMessage origem) {
        return TriagemFeedbackMessage.builder()
                .correlationId(origem.correlationId())
                .usuarioId(origem.usuarioId())
                .enderecoId(origem.enderecoId())
                .foto(origem.foto())
                .titulo(TriagemIaResult.truncarUtf8("Ocorrência registrada em " + origem.bairro(), TriagemIaResult.MAX_TITULO_BYTES))
                .descricao("Solicitação registrada com sucesso via aplicativo. Aguardando análise da equipe técnica municipal.")
                .categoria(CategoriaSugerida.VIAS_E_PAVIMENTACAO)
                .prioridade(PrioridadeChamado.MEDIA)
                .orgao(DepartamentoGoverno.SUBPREFEITURA)
                .status(StatusChamado.PENDENTE)
                .observacoes("Chamado registrado pelo cidadão via aplicativo. Aguardando triagem da equipe responsável.")
                .build();
    }

    public static TriagemFeedbackMessage erro(ChamadoTriagemMessage origem, String mensagem) {
        return TriagemFeedbackMessage.builder()
                .correlationId(origem.correlationId())
                .usuarioId(origem.usuarioId())
                .enderecoId(origem.enderecoId())
                .erro(mensagem)
                .build();
    }
}
