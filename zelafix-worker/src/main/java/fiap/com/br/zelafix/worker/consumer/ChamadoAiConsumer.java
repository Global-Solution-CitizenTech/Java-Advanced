package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.worker.queue.dto.TriagemFeedbackMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumidor Assíncrono RabbitMQ para Triagem Inteligente com Spring AI.
 *
 * Escuta a fila 'chamado.triagem.queue', analisa o chamado via LLM
 * e publica o parecer na fila de retorno 'chamado.triagem-concluida.queue'.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM)
    public void processarTriagem(ChamadoTriagemMessage message) {
        log.info("Processando triagem assíncrona com Spring AI para o chamado ID: {}", message.idChamado());

        TriagemIaResult resultado = aiService.analisarChamado(
                message.categoria(),
                message.titulo(),
                message.descricao(),
                message.bairro()
        );

        var feedback = new TriagemFeedbackMessage(
                message.idChamado(),
                resultado.prioridade().name(),
                resultado.orgaoResponsavel().getNomeOficial(),
                resultado.parecerIa()
        );

        rabbitTemplate.convertAndSend(
                RabbitWorkerConfig.EXCHANGE_NAME,
                RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA,
                feedback
        );

        log.info("Triagem IA concluída e publicada na fila de retorno para o chamado ID: {}", message.idChamado());
    }
}
