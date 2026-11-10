package fiap.com.br.zelafix.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de Mensageria Assíncrona com RabbitMQ (Opção A - Requisito Obrigatório da GS 2026/2).
 *
 * Estrutura:
 * - DirectExchange: zelafix.direct (Central de Triagem que recebe os eventos da API)
 * - Filas:
 *   1) chamado.triagem.queue: Fila durável que armazena ocorrências para triagem inteligente por IA.
 *   2) chamado.voto.queue: Fila durável que armazena os votos de apoio dos cidadãos para agregação em lote.
 */
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "zelafix.direct"; // Exchange: recebe e encaminha as mensagens

    public static final String QUEUE_CHAMADO_TRIAGEM = "chamado.triagem.queue"; // Fila de triagem por IA
    public static final String ROUTING_KEY_CHAMADO_TRIAGEM = "chamado.triagem.key"; // Routing Key para triagem

    public static final String QUEUE_CHAMADO_VOTO = "chamado.voto.queue"; // Fila de agregação de votos
    public static final String ROUTING_KEY_CHAMADO_VOTO = "chamado.voto.key"; // Routing Key para votos

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter("*");
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }

    // Queue (Fila durável) que armazena mensagens de ocorrências para triagem de IA
    @Bean
    public Queue triagemQueue() {
        return new Queue(QUEUE_CHAMADO_TRIAGEM, true);
    }

    // Queue (Fila durável) que armazena os votos dos cidadãos
    @Bean
    public Queue votoQueue() {
        return new Queue(QUEUE_CHAMADO_VOTO, true);
    }

    // Exchange Direct (Central de Triagem) que recebe as mensagens enviadas pelos produtores
    @Bean
    public DirectExchange zelafixExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    // Liga a Fila de Triagem à Exchange com a Routing Key
    @Bean
    public Binding triagemBinding() {
        return BindingBuilder
                .bind(triagemQueue())
                .to(zelafixExchange())
                .with(ROUTING_KEY_CHAMADO_TRIAGEM);
    }

    // Liga a Fila de Votos à Exchange com a Routing Key
    @Bean
    public Binding votoBinding() {
        return BindingBuilder
                .bind(votoQueue())
                .to(zelafixExchange())
                .with(ROUTING_KEY_CHAMADO_VOTO);
    }

    /*
            PRODUTOR (zelafix-api)
               │
               │ envia mensagem serializada em JSON
               │ routingKey = "chamado.triagem.key" / "chamado.voto.key"
               ▼
    ┌──────────────────────────────────┐
    │          zelafix.direct          │
    │             Exchange             │
    └─────────────────┬────────────────┘
                      │
                      │ verifica o Binding
          ┌───────────┴─────────────┐
          ▼                         ▼
    ┌──────────────────────┐  ┌────────────────────┐
    │ chamado.triagem.queue│  │ chamado.voto.queue │
    │        Queue         │  │       Queue        │
    └───────────┬──────────┘  └──────────┬─────────┘
                │                        │
                ▼                        ▼
        CONSUMIDOR WORKER        CONSUMIDOR WORKER
        (zelafix-worker)         (zelafix-worker)
    */
}
