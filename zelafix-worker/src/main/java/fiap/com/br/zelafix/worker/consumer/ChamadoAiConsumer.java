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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${AI_API_KEY:}")
    private String apiKey;

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM)
    public void processarTriagem(ChamadoTriagemMessage message) {
        log.info("Recebida mensagem de triagem [correlationId={}] para usuário ID: {} (bairro: {})", message.correlationId(), message.usuarioId(), message.bairro());

        TriagemFeedbackMessage feedback = gerarFeedback(message);

        rabbitTemplate.convertAndSend(
                RabbitWorkerConfig.EXCHANGE_NAME,
                RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA,
                feedback
        );

        log.info("Triagem concluída e publicada [correlationId={}, status={}, recusado={}] para usuário ID: {}", feedback.correlationId(), feedback.status(), feedback.recusado(), message.usuarioId());
    }

    private TriagemFeedbackMessage gerarFeedback(ChamadoTriagemMessage message) {
        if (apiKey.isBlank()) return TriagemFeedbackMessage.contingencia(message);
        try {
            log.info("Iniciando análise multimodal de IA para bairro: {} (correlationId={})", message.bairro(), message.correlationId());
            TriagemIaResult resultado = aiService.analisarChamado(message.bairro(), message.foto());
            if (!resultado.ocorrenciaValida()) {
                log.warn("Criação de chamado RECUSADA pela IA para usuário ID {} (bairro: {}): {}", message.usuarioId(), message.bairro(), resultado.descricao());
                return TriagemFeedbackMessage.recusa(message, resultado);
            }
            log.info("Triagem por IA concluída com sucesso para usuário ID {}: [{}] {}", message.usuarioId(), resultado.prioridade(), resultado.titulo());
            return TriagemFeedbackMessage.sucesso(message, resultado);
        } catch (Exception e) {
            log.error("Falha detalhada na IA ({}) para usuário ID {} [correlationId={}]: {}. Ativando triagem manual.", e.getClass().getSimpleName(), message.usuarioId(), message.correlationId(), e.getMessage());
            return TriagemFeedbackMessage.contingencia(message);
        }
    }
}
