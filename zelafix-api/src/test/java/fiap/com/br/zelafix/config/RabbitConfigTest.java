package fiap.com.br.zelafix.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:oracle:thin:@localhost:1521/xe",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class RabbitConfigTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private DirectExchange zelafixExchange;

    @Autowired
    private Queue triagemQueue;

    @Autowired
    private Queue votoQueue;

    @Autowired
    private Binding triagemBinding;

    @Autowired
    private Binding votoBinding;

    @Test
    @DisplayName("Deve inicializar as filas, exchanges e bindings do RabbitMQ corretamente")
    void deveConfigurarRabbitMQ() {
        assertNotNull(zelafixExchange);
        assertEquals(RabbitConfig.EXCHANGE_NAME, zelafixExchange.getName());

        assertNotNull(triagemQueue);
        assertEquals(RabbitConfig.QUEUE_CHAMADO_TRIAGEM, triagemQueue.getName());

        assertNotNull(votoQueue);
        assertEquals(RabbitConfig.QUEUE_CHAMADO_VOTO, votoQueue.getName());

        assertNotNull(triagemBinding);
        assertEquals(RabbitConfig.ROUTING_KEY_CHAMADO_TRIAGEM, triagemBinding.getRoutingKey());

        assertNotNull(votoBinding);
        assertEquals(RabbitConfig.ROUTING_KEY_CHAMADO_VOTO, votoBinding.getRoutingKey());
    }
}

