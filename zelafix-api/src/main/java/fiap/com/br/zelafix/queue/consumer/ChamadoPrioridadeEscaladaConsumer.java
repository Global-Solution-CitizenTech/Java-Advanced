package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.config.RabbitConfig;
import fiap.com.br.zelafix.queue.dto.PrioridadeEscaladaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consumidor de Retorno da Mensageria (Worker -> API).
 *
 * Processa solicitações de escalonamento comunitário emitidas pelo Worker quando uma ocorrência atinge limiares
 * de apoio popular no buffer (Padrão Paredão), aplicando a elevação de prioridade com persistência ACID no Oracle.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChamadoPrioridadeEscaladaConsumer {

    private final ChamadoService chamadoService;

    @RabbitListener(queues = RabbitConfig.QUEUE_CHAMADO_PRIORIDADE_ESCALADA)
    public void receberEscalonamentoPrioridade(PrioridadeEscaladaMessage message) {
        log.info("Recebido evento de escalonamento comunitário: chamado ID {} -> {} ({} votos)", message.idChamado(), message.novaPrioridade(), message.totalVotos());
        chamadoService.escalarPrioridadePorApoio(message);
    }
}
