package fiap.com.br.zelafix.queue.producer;

import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.ChamadoTriagemMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChamadoEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ChamadoEventProducer producer;

    @Test
    @DisplayName("Deve publicar evento de triagem com a exchange e routing key corretas")
    void devePublicarEventoDeTriagem() {
        ChamadoTriagemMessage msg = new ChamadoTriagemMessage(1L, 10L, "Centro", new byte[]{1, 2, 3});

        producer.publicarTriagem(msg);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq(RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM),
                eq(msg)
        );
    }

    @Test
    @DisplayName("Deve propagar AmqpException quando o RabbitMQ estiver indisponível")
    void devePropagarExcecaoDoRabbitMQ() {
        ChamadoTriagemMessage msg = new ChamadoTriagemMessage(1L, 10L, "Centro", new byte[]{1, 2, 3});

        doThrow(new AmqpException("Broker connection refused"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EXCHANGE_NAME), eq(RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM), eq(msg));

        assertThrows(AmqpException.class, () -> producer.publicarTriagem(msg));
    }
}
