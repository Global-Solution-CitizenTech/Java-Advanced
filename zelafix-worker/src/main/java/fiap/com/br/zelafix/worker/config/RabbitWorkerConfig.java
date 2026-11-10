package fiap.com.br.zelafix.worker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração RabbitMQ no Worker Consumidor
 *
 * Estrutura idempotente espelhando as filas e exchange da API:
 * - DirectExchange: zelafix.direct
 * - Fila de Triagem IA: chamado.triagem.queue
 * - Fila de Votos/Apoios: chamado.voto.queue
 */
@Configuration
public class RabbitWorkerConfig {

    public static final String EXCHANGE_NAME = "zelafix.direct";
    public static final String QUEUE_CHAMADO_TRIAGEM = "chamado.triagem.queue";
    public static final String ROUTING_KEY_CHAMADO_TRIAGEM = "chamado.triagem.key";

    public static final String QUEUE_CHAMADO_TRIAGEM_CONCLUIDA = "chamado.triagem-concluida.queue";
    public static final String ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA = "chamado.triagem-concluida.key";

    public static final String QUEUE_CHAMADO_VOTO = "chamado.voto.queue";
    public static final String ROUTING_KEY_CHAMADO_VOTO = "chamado.voto.key";

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter("*");
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    // Fila durável de triagem por IA
    @Bean
    public Queue triagemQueue() {
        return new Queue(QUEUE_CHAMADO_TRIAGEM, true);
    }

    // Fila durável de retorno da triagem por IA (Worker -> API)
    @Bean
    public Queue triagemConcluidaQueue() {
        return new Queue(QUEUE_CHAMADO_TRIAGEM_CONCLUIDA, true);
    }

    // Fila durável de votos/apoios de cidadãos
    @Bean
    public Queue votoQueue() {
        return new Queue(QUEUE_CHAMADO_VOTO, true);
    }

    // Exchange Direct (Central de Triagem)
    @Bean
    public DirectExchange zelafixExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // Estabelece a ligação entre a fila de triagem e a Exchange
    @Bean
    public Binding triagemBinding() {
        return BindingBuilder
                .bind(triagemQueue())
                .to(zelafixExchange())
                .with(ROUTING_KEY_CHAMADO_TRIAGEM);
    }

    // Estabelece a ligação entre a fila de retorno e a Exchange
    @Bean
    public Binding triagemConcluidaBinding() {
        return BindingBuilder
                .bind(triagemConcluidaQueue())
                .to(zelafixExchange())
                .with(ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA);
    }

    // Estabelece a ligação entre a fila de votos e a Exchange
    @Bean
    public Binding votoBinding() {
        return BindingBuilder
                .bind(votoQueue())
                .to(zelafixExchange())
                .with(ROUTING_KEY_CHAMADO_VOTO);
    }

}
