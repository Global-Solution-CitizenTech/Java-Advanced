package fiap.com.br.zelafix.worker.config;

import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitWorkerConfigTest {

    private RabbitWorkerConfig rabbitConfig;

    @BeforeEach
    void setUp() {
        rabbitConfig = new RabbitWorkerConfig();
    }

    @Test
    @DisplayName("Deve configurar o MessageConverter com JacksonJsonMessageConverter")
    void deveConfigurarJacksonJsonMessageConverter() {
        MessageConverter converter = rabbitConfig.jsonMessageConverter();
        assertNotNull(converter);
        assertInstanceOf(JacksonJsonMessageConverter.class, converter);
    }

    @Test
    @DisplayName("Deve configurar a fila de triagem com nome correto e durabilidade")
    void deveConfigurarFilaTriagem() {
        Queue queue = rabbitConfig.triagemQueue();
        assertNotNull(queue);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM, queue.getName());
        assertEquals("chamado.triagem.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("Deve configurar a fila de retorno da triagem com nome correto e durabilidade")
    void deveConfigurarFilaTriagemConcluida() {
        Queue queue = rabbitConfig.triagemConcluidaQueue();
        assertNotNull(queue);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA, queue.getName());
        assertEquals("chamado.triagem-concluida.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("Deve configurar a fila de voto com nome correto e durabilidade")
    void deveConfigurarFilaVoto() {
        Queue queue = rabbitConfig.votoQueue();
        assertNotNull(queue);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_VOTO, queue.getName());
        assertEquals("chamado.voto.queue", queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("Deve configurar a Exchange zelafix.direct como Direct e durável")
    void deveConfigurarZelafixExchange() {
        DirectExchange exchange = rabbitConfig.zelafixExchange();
        assertNotNull(exchange);
        assertEquals(RabbitWorkerConfig.EXCHANGE_NAME, exchange.getName());
        assertEquals("zelafix.direct", exchange.getName());
        assertTrue(exchange.isDurable());
        assertEquals("direct", exchange.getType());
    }

    @Test
    @DisplayName("Deve configurar o binding da fila de triagem com a Exchange e Routing Key corretas")
    void deveConfigurarTriagemBinding() {
        Binding binding = rabbitConfig.triagemBinding();
        assertNotNull(binding);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM, binding.getDestination());
        assertEquals(RabbitWorkerConfig.EXCHANGE_NAME, binding.getExchange());
        assertEquals(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM, binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    @DisplayName("Deve configurar o binding da fila de retorno com a Exchange e Routing Key corretas")
    void deveConfigurarTriagemConcluidaBinding() {
        Binding binding = rabbitConfig.triagemConcluidaBinding();
        assertNotNull(binding);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM_CONCLUIDA, binding.getDestination());
        assertEquals(RabbitWorkerConfig.EXCHANGE_NAME, binding.getExchange());
        assertEquals(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA, binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    @DisplayName("Deve configurar o binding da fila de voto com a Exchange e Routing Key corretas")
    void deveConfigurarVotoBinding() {
        Binding binding = rabbitConfig.votoBinding();
        assertNotNull(binding);
        assertEquals(RabbitWorkerConfig.QUEUE_CHAMADO_VOTO, binding.getDestination());
        assertEquals(RabbitWorkerConfig.EXCHANGE_NAME, binding.getExchange());
        assertEquals(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_VOTO, binding.getRoutingKey());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    @DisplayName("Deve serializar e desserializar ChamadoTriagemMessage via AMQP MessageConverter")
    void deveSerializarEDesserializarChamadoTriagemMessage() {
        JacksonJsonMessageConverter converter = (JacksonJsonMessageConverter) rabbitConfig.jsonMessageConverter();

        ChamadoTriagemMessage original = new ChamadoTriagemMessage(
                999L,
                "Buraco profundo na avenida",
                "Asfalto cedeu gerando cratera",
                "Pavimentação",
                "Pinheiros"
        );

        Message message = converter.toMessage(original, new MessageProperties());
        assertNotNull(message);
        assertNotNull(message.getBody());
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());

        Object result = converter.fromMessage(message);
        assertNotNull(result);
        assertInstanceOf(ChamadoTriagemMessage.class, result);

        ChamadoTriagemMessage deserialized = (ChamadoTriagemMessage) result;
        assertEquals(original.idChamado(), deserialized.idChamado());
        assertEquals(original.titulo(), deserialized.titulo());
        assertEquals(original.descricao(), deserialized.descricao());
        assertEquals(original.categoria(), deserialized.categoria());
        assertEquals(original.bairro(), deserialized.bairro());
    }

    @Test
    @DisplayName("Deve serializar e desserializar VotoMessage via AMQP MessageConverter")
    void deveSerializarEDesserializarVotoMessage() {
        JacksonJsonMessageConverter converter = (JacksonJsonMessageConverter) rabbitConfig.jsonMessageConverter();

        VotoMessage original = new VotoMessage(500L, 123L, "ADICIONAR");

        Message message = converter.toMessage(original, new MessageProperties());
        assertNotNull(message);
        assertEquals(MessageProperties.CONTENT_TYPE_JSON, message.getMessageProperties().getContentType());

        Object result = converter.fromMessage(message);
        assertNotNull(result);
        assertInstanceOf(VotoMessage.class, result);

        VotoMessage deserialized = (VotoMessage) result;
        assertEquals(original.idChamado(), deserialized.idChamado());
        assertEquals(original.idUsuario(), deserialized.idUsuario());
        assertEquals(original.operacao(), deserialized.operacao());
    }

    @Test
    @DisplayName("Deve falhar com MessageConversionException ao receber JSON inválido")
    void deveFalharComJsonInvalido() {
        JacksonJsonMessageConverter converter = (JacksonJsonMessageConverter) rabbitConfig.jsonMessageConverter();

        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setHeader("__TypeId__", "fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage");

        Message malformedMessage = new Message("ESTE_NAO_E_UM_JSON_VALIDO".getBytes(StandardCharsets.UTF_8), props);

        assertThrows(MessageConversionException.class, () -> converter.fromMessage(malformedMessage));
    }
}
