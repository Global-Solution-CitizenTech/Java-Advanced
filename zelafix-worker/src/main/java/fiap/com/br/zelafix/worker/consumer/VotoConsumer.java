package fiap.com.br.zelafix.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor Assíncrono e Agregador de Votos RabbitMQ (Opção A).
 * Inspirado 100% no padrão 'VoteConsumer' do projeto 'paredao' ministrado em aula pelo professor.
 *
 * Em estrita 3FN, a contagem de apoios é calculada dinamicamente via relação 'voto_chamado'
 * (sem redundância de coluna física 'apoios_count' na tabela 'chamado').
 * Este consumidor gerencia o buffer concorrente em memória e consolida a telemetria em lote.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VotoConsumer {

    private final ObjectMapper objectMapper;

    // ConcurrentHashMap thread-safe para acumular os votos em memória
    // Chave: ID do chamado, Valor: saldo líquido acumulado de apoios
    private final ConcurrentHashMap<Long, Integer> votes = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_VOTO)
    public void consumeVoto(String messageJson) {
        try {
            VotoMessage message = objectMapper.readValue(messageJson, VotoMessage.class);
            consumeVoto(message);
        } catch (Exception e) {
            log.error(">>> [ZelaFix Worker] Erro ao processar mensagem de voto JSON: {}", e.getMessage());
        }
    }

    public void consumeVoto(VotoMessage message) {
        int delta = "REMOVER".equalsIgnoreCase(message.tipo()) ? -1 : 1;
        votes.merge(message.idChamado(), delta, Integer::sum);
        log.info(">>> [ZelaFix Worker] Voto ({}) recebido para o chamado ID: {}, saldo no buffer: {}",
                message.tipo(), message.idChamado(), votes.get(message.idChamado()));
    }

    @Scheduled(fixedDelay = 3_000)
    public void flush() {
        if (votes.isEmpty()) return;

        log.info(">>> [ZelaFix Worker] Flush dos votos: consolidando lote acumulado no buffer...");
        votes.forEach((chamadoId, totalVotos) -> {
            log.info(">>> [ZelaFix Worker] Chamado ID {} consolidado com saldo de {} apoios no buffer.", chamadoId, totalVotos);
        });
        votes.clear();
    }

    public ConcurrentHashMap<Long, Integer> getVotesBuffer() {
        return votes;
    }
}
