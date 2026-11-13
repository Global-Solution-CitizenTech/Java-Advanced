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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        ChamadoTriagemMessage msg = new ChamadoTriagemMessage(10L, "Centro", new byte[]{1, 2, 3});

        producer.publicarTriagem(msg);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.EXCHANGE_NAME),
                eq(RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM),
                eq(msg)
        );
    }

    @Test
    @DisplayName("Deve capturar exceção do RabbitMQ sem quebrar o fluxo chamador (fallback resiliente)")
    void deveCapturarFalhaDoRabbitMQComResiliencia() {
        ChamadoTriagemMessage msg = new ChamadoTriagemMessage(10L, "Centro", new byte[]{1, 2, 3});

        doThrow(new AmqpException("Broker connection refused"))
                .when(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EXCHANGE_NAME), eq(RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM), eq(msg));

        assertDoesNotThrow(() -> producer.publicarTriagem(msg));
    }
}
