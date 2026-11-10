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
 * Escuta a fila 'chamado.triagem-concluida.queue', recebe o parecer processado pelo worker
 * e atualiza o histórico do chamado no banco Oracle para que o app mobile possa exibir os dados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoTriagemFeedbackConsumer {

    private final ChamadoService chamadoService;

    @RabbitListener(queues = RabbitConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA)
    public void receberFeedbackTriagem(TriagemFeedbackMessage message) {
        log.info("Recebido parecer da IA para chamado ID {}: [{}] {}", message.idChamado(), message.prioridade(), message.orgao());
        chamadoService.aplicarTriagemIa(message.idChamado(), message.prioridade(), message.orgao(), message.parecer());
    }
}
