package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor Assíncrono RabbitMQ para Retorno da Triagem Inteligente.
 *
 * Escuta a fila 'chamado.triagem-concluida.queue', recebe os dados gerados pelo worker
 * e atualiza título, descrição, categoria, prioridade e órgão do chamado no banco Oracle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoTriagemFeedbackConsumer {

    private final ChamadoService chamadoService;

    @RabbitListener(queues = RabbitConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA)
    public void receberFeedbackTriagem(TriagemFeedbackMessage feedback) {
        log.info("Recebida triagem da IA para chamado ID {}: [{}] {} - {}", feedback.idChamado(), feedback.prioridadeNome(), feedback.orgaoNome(), feedback.categoriaNome());
        chamadoService.aplicarTriagemIa(feedback);
    }
}
