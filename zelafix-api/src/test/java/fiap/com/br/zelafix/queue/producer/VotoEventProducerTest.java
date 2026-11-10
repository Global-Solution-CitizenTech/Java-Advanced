package fiap.com.br.zelafix.queue.producer;

import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.VotoMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VotoEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private VotoEventProducer producer;

    @Test
    @DisplayName("Deve publicar mensagem de voto com exchange e routing key corretas")
    void devePublicarVoto() {
        VotoMessage msg = new VotoMessage(10L, 1L, "ADICIONAR");

        producer.publicarVoto(msg);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq(RabbitConfig.ROUTING_KEY_CHAMADO_VOTO),
                eq(msg)
        );
    }

    @Test
    @DisplayName("Deve capturar exceção do RabbitMQ sem lançar erro ao chamador")
    void deveCapturarFalhaDoRabbitMQComResiliencia() {
        VotoMessage msg = new VotoMessage(10L, 1L, "REMOVER");

        doThrow(new AmqpException("Broker down"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EXCHANGE_NAME), eq(RabbitConfig.ROUTING_KEY_CHAMADO_VOTO), eq(msg));

        assertDoesNotThrow(() -> producer.publicarVoto(msg));
    }
}
