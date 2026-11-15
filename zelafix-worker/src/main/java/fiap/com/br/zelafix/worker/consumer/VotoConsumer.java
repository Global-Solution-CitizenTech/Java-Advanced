package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.PrioridadeEscaladaMessage;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor Assíncrono e Motor de Escalonamento Comunitário de Prioridade (Padrão Paredão).
 *
 * Agrega rajadas de apoios em buffer thread-safe em memória (ConcurrentHashMap) e avalia periodicamente (a cada 3s)
 * se a ocorrência atingiu o limiar de mobilização popular para solicitar escalonamento à API via RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VotoConsumer {

    public static final int LIMIAR_ALTA = 3;
    public static final int LIMIAR_URGENTE = 5;

    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentHashMap<Long, Integer> votos = new ConcurrentHashMap<>();

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_VOTO)
    public void consumeVoto(VotoMessage message) {
        if ("REMOVER".equalsIgnoreCase(message.operacao())) {
            votos.remove(message.idChamado());
        } else {
            votos.put(message.idChamado(), message.totalVotos());
        }
    }

    @Scheduled(fixedDelay = 3_000)
    public void flush() {
        votos.forEach((idChamado, total) -> {
            votos.remove(idChamado);
            if (total >= LIMIAR_ALTA) {
                PrioridadeChamado nova = total >= LIMIAR_URGENTE ? PrioridadeChamado.URGENTE : PrioridadeChamado.ALTA;
                rabbitTemplate.convertAndSend(
                        RabbitWorkerConfig.EXCHANGE_NAME,
                        RabbitWorkerConfig.ROUTING_KEY_CHAMADO_PRIORIDADE_ESCALADA,
                        new PrioridadeEscaladaMessage(idChamado, nova, total, "Escalonamento comunitário: %d apoios atingidos.".formatted(total))
                );
                log.info("Escalonamento solicitado: Chamado {} -> {} ({} votos)", idChamado, nova, total);
            }
        });
    }

    public ConcurrentHashMap<Long, Integer> getVotesBuffer() {
        return votos;
    }
}
