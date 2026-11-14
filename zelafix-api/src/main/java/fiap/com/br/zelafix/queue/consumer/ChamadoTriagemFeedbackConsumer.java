package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.config.RabbitConfig;
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

    @RabbitListener(queues = RabbitConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA)
    public void receberFeedbackTriagem(TriagemFeedbackMessage feedback) {
        if (feedback.recusado()) {
            log.warn("Triagem recusada pela IA para usuário ID {}: {}", feedback.usuarioId(), feedback.descricao());
            chamadoService.registrarRecusaIa(feedback.usuarioId(), feedback.descricao());
            return;
        }
        log.info("Recebida triagem da IA para usuário ID {}: [{}] {} - {}", feedback.usuarioId(), feedback.prioridadeNome(), feedback.orgaoNome(), feedback.categoriaNome());
        chamadoService.aplicarTriagemIa(feedback);
    }
}
