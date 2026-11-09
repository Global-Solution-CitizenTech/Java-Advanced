package fiap.com.br.zelafix.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publicarTriagem(ChamadoTriagemMessage message) {
        log.info("Publicando evento de triagem no RabbitMQ para o chamado ID: {}", message.idChamado());
        try {
            String json = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM,
                    json
            );
        } catch (Exception e) {
            log.warn("RabbitMQ offline ou falha ao publicar mensagem de triagem ({}). O processamento seguirá em modo síncrono/fallback.", e.getMessage());
        }
    }
}
