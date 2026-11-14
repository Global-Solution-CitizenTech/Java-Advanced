package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.chamado.TriagemPendenteStore;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.exception.TriagemIndisponivelException;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoTriagemFeedbackConsumer {

    private final ChamadoService chamadoService;
    private final TriagemPendenteStore triagemPendenteStore;

    @RabbitListener(queues = RabbitConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA)
    public void receberFeedbackTriagem(TriagemFeedbackMessage feedback) {
        if (feedback.erro() != null && !feedback.erro().isBlank()) {
            log.error("Erro retornado pelo worker na triagem para usuário ID {}: {}", feedback.usuarioId(), feedback.erro());
            triagemPendenteStore.falhar(feedback.correlationId(), new TriagemIndisponivelException(feedback.erro()));
            return;
        }
        if (feedback.recusado()) {
            log.warn("Triagem recusada pela IA para usuário ID {}: {}", feedback.usuarioId(), feedback.descricao());
            triagemPendenteStore.rejeitar(feedback.correlationId(), feedback.descricao());
            return;
        }
        try {
            log.info("Recebida triagem da IA para usuário ID {}: [{}] {} - {}", feedback.usuarioId(), feedback.prioridadeNome(), feedback.orgaoNome(), feedback.categoriaNome());
            ChamadoResponse criado = chamadoService.aplicarTriagemIa(feedback);
            triagemPendenteStore.concluir(feedback.correlationId(), criado);
        } catch (RuntimeException e) {
            log.error("Falha ao persistir chamado após triagem para usuário ID {}: {}", feedback.usuarioId(), e.getMessage(), e);
            triagemPendenteStore.falhar(feedback.correlationId(), e);
        }
    }
}
