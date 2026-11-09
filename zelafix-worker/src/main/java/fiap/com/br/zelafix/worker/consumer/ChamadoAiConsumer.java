package fiap.com.br.zelafix.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumidor Assíncrono RabbitMQ para Triagem Inteligente com Spring AI.
 *
 * Escuta a fila 'chamado.triagem.queue', recebe o evento gerado pela API quando o cidadão
 * abre uma ocorrência, aciona a IA Generativa e registra o parecer no histórico do chamado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM)
    public void processarTriagem(String messageJson) {
        try {
            ChamadoTriagemMessage message = objectMapper.readValue(messageJson, ChamadoTriagemMessage.class);
            executarTriagem(message);
        } catch (Exception e) {
            log.error(">>> [ZelaFix Worker] Erro ao desserializar mensagem de triagem JSON: {}", e.getMessage());
        }
    }

    public void processarTriagem(ChamadoTriagemMessage message) {
        executarTriagem(message);
    }

    private void executarTriagem(ChamadoTriagemMessage message) {
        log.info(">>> [ZelaFix Worker] Mensagem recebida na fila de triagem. Chamado ID: {}", message.idChamado());

        TriagemIaResult resultado = aiService.analisarChamado(
                message.categoria(),
                message.titulo(),
                message.descricao(),
                message.bairro()
        );

        String observacao = String.format("[Triagem Spring AI - Órgão: %s | Urgência: %s]",
                resultado.orgaoResponsavel(), resultado.prioridade());
        if (observacao.length() > 255) {
            observacao = observacao.substring(0, 255);
        }

        log.info(">>> [ZelaFix Worker] Triagem por IA concluída para o chamado ID: {}. Parecer: {}",
                message.idChamado(), observacao);

        // Atualiza a observação no último registro de histórico do chamado
        try {
            jdbcTemplate.update("""
                UPDATE hist_chamado
                SET observacoes = ?
                WHERE id_hist = (
                    SELECT MAX(h.id_hist)
                    FROM hist_chamado h
                    WHERE h.chamado_id_chamado = ?
                )
            """, observacao, message.idChamado());
        } catch (Exception e) {
            log.error(">>> [ZelaFix Worker] Falha ao atualizar histórico do chamado ID {}: {}", message.idChamado(), e.getMessage());
        }
    }
}
