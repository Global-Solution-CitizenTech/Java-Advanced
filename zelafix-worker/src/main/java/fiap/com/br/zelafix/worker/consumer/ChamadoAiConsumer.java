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
import org.springframework.stereotype.Component;

/**
 * Consumidor Assíncrono RabbitMQ para Triagem Inteligente com Spring AI (Opção A).
 *
 * Escuta a fila 'chamado.triagem.queue', recebe o evento gerado pela API quando o cidadão
 * abre uma ocorrência, aciona o serviço de IA Generativa e atualiza os dados no banco.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoAiConsumer {

    private final ZelaFixAiService aiService;
    private final ChamadoWorkerRepository chamadoWorkerRepository;
    private final ObjectMapper objectMapper;

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

        chamadoWorkerRepository.findById(message.idChamado()).ifPresentOrElse(chamado -> {
            chamado.setPrioridade(resultado.prioridade());
            chamado.setOrgaoResponsavel(resultado.orgaoResponsavel());
            chamado.setParecerIa(resultado.parecerIa());
            chamadoWorkerRepository.save(chamado);

            log.info(">>> [ZelaFix Worker] Triagem por IA concluída e salva no banco para o chamado ID: {}. Prioridade: {}, Órgão: {}",
                    chamado.getIdChamado(), chamado.getPrioridade(), chamado.getOrgaoResponsavel());
        }, () -> log.warn(">>> [ZelaFix Worker] Chamado ID {} não localizado no banco para triagem.", message.idChamado()));
    }
}
