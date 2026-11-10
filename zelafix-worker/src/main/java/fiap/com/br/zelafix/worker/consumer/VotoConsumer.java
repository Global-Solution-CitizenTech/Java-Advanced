package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor Assíncrono e Agregador de Votos RabbitMQ (Padrão Paredão).
 *
 * Utiliza ConcurrentHashMap em memória para agregação de alta vazão e flush periódico a cada 3s.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VotoConsumer {

    private final ConcurrentHashMap<Long, Integer> votes = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_VOTO)
    public void consumeVoto(VotoMessage message) {
        int delta = "REMOVER".equalsIgnoreCase(message.operacao()) ? -1 : 1;
        votes.merge(message.idChamado(), delta, Integer::sum);
        log.info("Voto ({}) computado para o chamado ID: {}, saldo no buffer: {}",
                message.operacao(), message.idChamado(), votes.get(message.idChamado()));
    }

    @Scheduled(fixedDelay = 3_000)
    public void flush() {
        if (votes.isEmpty()) return;

        log.info("Flush dos votos: consolidando lote de {} chamados no buffer...", votes.size());
        votes.forEach((id, total) -> log.info("Chamado ID {} com saldo de {} apoios computados.", id, total));
        votes.clear();
    }

    public ConcurrentHashMap<Long, Integer> getVotesBuffer() {
        return votes;
    }
}
