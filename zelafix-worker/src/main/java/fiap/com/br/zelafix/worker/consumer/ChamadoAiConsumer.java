package fiap.com.br.zelafix.worker.consumer;

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
 * Escuta a fila 'chamado.triagem.queue', recebe o evento tipado gerado pela API,
 * aciona o modelo multimodal Qwen 3.8 27B e persiste o parecer no histórico do chamado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final JdbcTemplate jdbcTemplate;

    @RabbitListener(queues = RabbitWorkerConfig.QUEUE_CHAMADO_TRIAGEM)
    public void processarTriagem(ChamadoTriagemMessage message) {
        if (message == null || message.idChamado() == null) {
            log.warn("Mensagem de triagem nula ou sem idChamado descartada: {}", message);
            return;
        }
        log.info("Processando triagem assíncrona com Spring AI para o chamado ID: {}", message.idChamado());

        byte[] fotoBytes = carregarFoto(message.idChamado());

        TriagemIaResult resultado = aiService.analisarChamado(
                message.categoria(),
                message.titulo(),
                message.descricao(),
                message.bairro(),
                fotoBytes
        );

        atualizarHistoricoChamado(message.idChamado(), resultado);
    }

    private byte[] carregarFoto(Long idChamado) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT foto FROM chamado WHERE id_chamado = ?",
                    (rs, rowNum) -> rs.getBytes("foto"),
                    idChamado
            );
        } catch (Exception e) {
            log.debug("Foto não encontrada para o chamado ID {}: {}", idChamado, e.getMessage());
            return null;
        }
    }

    private void atualizarHistoricoChamado(Long idChamado, TriagemIaResult resultado) {
        String observacao = String.format("[Triagem IA - %s | %s] %s",
                resultado.prioridade(),
                resultado.orgaoResponsavel().getNomeOficial(),
                resultado.parecerIa());

        if (observacao.length() > 255) {
            observacao = observacao.substring(0, 252) + "...";
        }

        log.info("Triagem por IA concluída para o chamado ID {}: {}", idChamado, observacao);

        jdbcTemplate.update("""
            UPDATE hist_chamado
            SET observacoes = ?
            WHERE id_hist = (
                SELECT MAX(h.id_hist)
                FROM hist_chamado h
                WHERE h.chamado_id_chamado = ?
            )
        """, observacao, idChamado);
    }
}
