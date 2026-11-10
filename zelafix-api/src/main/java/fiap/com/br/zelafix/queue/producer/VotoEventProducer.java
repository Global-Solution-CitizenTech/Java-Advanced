package fiap.com.br.zelafix.queue.producer;

import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class VotoEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publicarVoto(VotoMessage message) {
        log.info("Publicando voto de apoio no RabbitMQ para o chamado ID: {}", message.idChamado());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.EXCHANGE_NAME,
                    RabbitConfig.ROUTING_KEY_CHAMADO_VOTO,
                    message
            );
        } catch (Exception e) {
            log.warn("RabbitMQ offline ou falha ao publicar voto de apoio ({}).", e.getMessage());
        }
    }
}
