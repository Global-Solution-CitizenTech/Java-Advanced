package fiap.com.br.zelafix.worker.queue.dto;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;

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
    public static TriagemFeedbackMessage sucesso(ChamadoTriagemMessage origem, TriagemIaResult resultado) {
        return new TriagemFeedbackMessage(
                origem.usuarioId(),
                origem.enderecoId(),
                origem.foto(),
                resultado.titulo(),
                resultado.descricao(),
                resultado.categoriaSugerida().name(),
                resultado.prioridade().name(),
                resultado.orgaoResponsavel().name(),
                "EM_ANALISE",
                false
        );
    }

    public static TriagemFeedbackMessage recusa(ChamadoTriagemMessage origem, TriagemIaResult resultado) {
        return new TriagemFeedbackMessage(
                origem.usuarioId(),
                origem.enderecoId(),
                null,
                resultado.titulo(),
                resultado.descricao(),
                null,
                null,
                null,
                null,
                true
        );
    }

    public static TriagemFeedbackMessage contingencia(ChamadoTriagemMessage origem) {
        return new TriagemFeedbackMessage(
                origem.usuarioId(),
                origem.enderecoId(),
                origem.foto(),
                "Ocorrência registrada em " + origem.bairro(),
                "Chamado registrado em modo de contingência. Aguardando triagem manual.",
                CategoriaSugerida.VIAS_E_PAVIMENTACAO.name(),
                PrioridadeChamado.MEDIA.name(),
                DepartamentoGoverno.SUBPREFEITURA.name(),
                "PENDENTE",
                false
        );
    }
}
