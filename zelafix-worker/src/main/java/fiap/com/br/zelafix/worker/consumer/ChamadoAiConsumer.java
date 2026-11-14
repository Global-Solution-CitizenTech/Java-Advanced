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

@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM)
    public void processarTriagem(ChamadoTriagemMessage message) {
        log.info("Processando triagem assíncrona com Spring AI para usuário ID: {}", message.usuarioId());

        TriagemFeedbackMessage feedback = gerarFeedback(message);

        rabbitTemplate.convertAndSend(
                RabbitWorkerConfig.EXCHANGE_NAME,
                RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA,
                feedback
        );

        log.info("Triagem concluída (status={}) e publicada para usuário ID: {}", feedback.statusNome(), message.usuarioId());
    }

    private TriagemFeedbackMessage gerarFeedback(ChamadoTriagemMessage message) {
        try {
            TriagemIaResult resultado = aiService.analisarChamado(message.bairro(), message.foto());
            return TriagemFeedbackMessage.sucesso(message, resultado);
        } catch (Exception e) {
            log.error("Falha na IA durante triagem para usuário ID {}. Aplicando contingência PENDENTE: {}", message.usuarioId(), e.getMessage());
            return TriagemFeedbackMessage.contingencia(message);
        }
    }
}
