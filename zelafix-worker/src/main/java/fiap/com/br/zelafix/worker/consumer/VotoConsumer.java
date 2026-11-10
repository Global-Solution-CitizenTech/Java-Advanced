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
 * Consumidor Assíncrono e Agregador de Votos RabbitMQ.
 *
 * Em estrita 3FN, a contagem de apoios é calculada dinamicamente via relação 'voto_chamado'.
 * Este consumidor gerencia o buffer concorrente em memória e consolida a telemetria em lote.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VotoConsumer {

    // ConcurrentHashMap thread-safe para acumular os votos em memória
    // Chave: ID do chamado, Valor: saldo líquido acumulado de apoios
    private final ConcurrentHashMap<Long, Integer> votes = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_VOTO)
    public void consumeVoto(VotoMessage message) {
        if (message == null || message.idChamado() == null) {
            log.warn("Mensagem de voto nula ou sem idChamado descartada: {}", message);
            return;
        }
        int delta = "REMOVER".equalsIgnoreCase(message.operacao()) ? -1 : 1;
        votes.merge(message.idChamado(), delta, Integer::sum);
        log.info("Voto ({}) recebido para o chamado ID: {}, saldo no buffer: {}",
                message.operacao(), message.idChamado(), votes.get(message.idChamado()));
    }

    @Scheduled(fixedDelay = 3_000)
    public void flush() {
        if (votes.isEmpty()) return;

        log.info("Flush dos votos: consolidando lote acumulado no buffer...");
        votes.forEach((chamadoId, totalVotos) -> {
            log.info("Chamado ID {} consolidado com saldo de {} apoios no buffer.", chamadoId, totalVotos);
        });
        votes.clear();
    }

    public ConcurrentHashMap<Long, Integer> getVotesBuffer() {
        return votes;
    }
}
