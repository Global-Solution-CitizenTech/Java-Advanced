package fiap.com.br.zelafix.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.worker.repository.ChamadoWorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Consumidor Assíncrono RabbitMQ para Triagem Inteligente com Spring AI (Opção A).
 *
 * Escuta a fila 'chamado.triagem.queue', recebe o evento gerado pela API quando o cidadão
 * abre uma ocorrência, aciona a IA Generativa e registra o parecer no histórico do chamado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final ChamadoWorkerRepository chamadoWorkerRepository;
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
        log.info(">>> [ZelaFix Worker] Mensagem recebida na fila de triagem. Chamado ID: {}, Protocolo: {}",
                message.idChamado(), message.protocolo());

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
                message.idChamado(), resultado.parecerIa());

        try {
            // Registra o parecer da IA no hist_chamado conforme regra do Edital GS2
            jdbcTemplate.update("""
                INSERT INTO hist_chamado (dt_mudanca, observacoes, chamado_id_chamado, usuario_id_usuario, status_id_status)
                SELECT SYSDATE, ?, c.id_chamado, c.usuario_id_usuario, 2
                FROM chamado c WHERE c.id_chamado = ?
            """, observacao, message.idChamado());
            log.info(">>> [ZelaFix Worker] Histórico de triagem registrado no Oracle para chamado ID: {}", message.idChamado());
        } catch (Exception e) {
            log.warn(">>> [ZelaFix Worker] Aviso ao persistir histórico no banco relacional: {}", e.getMessage());
        }
    }
}
