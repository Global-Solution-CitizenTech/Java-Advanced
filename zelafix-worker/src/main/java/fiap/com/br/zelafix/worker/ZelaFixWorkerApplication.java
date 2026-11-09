package fiap.com.br.zelafix.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Microsserviço 2: ZelaFix Worker (Consumidor Assíncrono RabbitMQ - Opção A)
 *
 * Responsabilidades:
 * 1. Consumir eventos de triagem da fila 'chamado.triagem.queue' e executar análise via Spring AI.
 * 2. Consumir votos da fila 'chamado.voto.queue' e agregar via buffer em memória (ConcurrentHashMap)
 *    com flush periódico para o banco de dados.
 */
@SpringBootApplication
@EnableScheduling
public class ZelaFixWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZelaFixWorkerApplication.class, args);
    }
}

