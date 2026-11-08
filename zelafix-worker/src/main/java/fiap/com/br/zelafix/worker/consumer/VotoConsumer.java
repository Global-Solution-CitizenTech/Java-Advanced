package fiap.com.br.zelafix.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import fiap.com.br.zelafix.worker.repository.ChamadoWorkerRepository;
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
 * Estrutura Didática:
 * - Concurrent -> Suporta múltiplas threads acessando e atualizando a mesma chave de forma segura.
 * - Hash -> Utiliza estrutura baseada em tabela hash para busca e inserção em O(1).
 * - Map -> Estrutura de chave (ID do chamado) e valor (quantidade de votos acumulados).
 *
 * Funcionamento:
 * 1. Recebe mensagens da fila 'chamado.voto.queue' via @RabbitListener.
 * 2. Acumula os votos no mapa em memória 'ConcurrentHashMap' para suportar picos de tráfego.
 * 3. A cada 3 segundos, o método 'flush()' descarrega os votos acumulados em lote no banco.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VotoConsumer {

    private final ChamadoWorkerRepository chamadoWorkerRepository;
    private final ObjectMapper objectMapper;

    // ConcurrentHashMap thread-safe para acumular os votos em memória
    // Chave: ID do chamado, Valor: quantidade acumulada de votos
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

    // Recebe vários votos durante o intervalo e, a cada execução, persiste no banco os votos acumulados no mapa
    @Scheduled(fixedDelay = 3_000) // Executa a cada 3 segundos
    public void flush() {
        if (votes.isEmpty()) return;

        log.info(">>> [ZelaFix Worker] Flush dos votos: persistindo lote acumulado no banco de dados...");
        votes.forEach(this::persistirVotos);
        votes.clear(); // Limpa os votos temporários do mapa
    }

    private void persistirVotos(Long chamadoId, Integer totalVotos) {
        chamadoWorkerRepository.findById(chamadoId).ifPresentOrElse(chamado -> {
            int atual = (chamado.getApoiosCount() != null) ? chamado.getApoiosCount() : 0;
            int novoTotal = Math.max(0, atual + totalVotos);
            chamado.setApoiosCount(novoTotal);
            chamadoWorkerRepository.save(chamado);
            log.info(">>> [ZelaFix Worker] Chamado ID {} atualizado no banco com variação de {} apoios (Total: {})",
                    chamadoId, totalVotos, chamado.getApoiosCount());
        }, () -> log.warn(">>> [ZelaFix Worker] Chamado ID {} não localizado no banco para flush de votos.", chamadoId));
    }

    // Método acessível para testes unitários
    public ConcurrentHashMap<Long, Integer> getVotesBuffer() {
        return votes;
    }
}
