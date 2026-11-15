package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.PrioridadeEscaladaMessage;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VotoConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private VotoConsumer votoConsumer;

    @Test
    @DisplayName("Deve publicar escalonamento para ALTA quando atingir limiar de 3 votos")
    void deveEscalarParaAltaQuandoAtingirTresVotos() {
        votoConsumer.consumeVoto(new VotoMessage(10L, 1L, "ADICIONAR", 1));
        votoConsumer.consumeVoto(new VotoMessage(10L, 2L, "ADICIONAR", 2));
        votoConsumer.consumeVoto(new VotoMessage(10L, 3L, "ADICIONAR", 3));

        votoConsumer.flush();

        ArgumentCaptor<PrioridadeEscaladaMessage> captor = ArgumentCaptor.forClass(PrioridadeEscaladaMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitWorkerConfig.EXCHANGE_NAME),
                eq(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_PRIORIDADE_ESCALADA),
                captor.capture()
        );
        assertEquals(10L, captor.getValue().idChamado());
        assertEquals(PrioridadeChamado.ALTA, captor.getValue().novaPrioridade());
        assertEquals(3, captor.getValue().totalVotos());
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }

    @Test
    @DisplayName("Deve publicar escalonamento para URGENTE quando atingir limiar de 5 votos")
    void deveEscalarParaUrgenteQuandoAtingirCincoVotos() {
        votoConsumer.consumeVoto(new VotoMessage(15L, 5L, "ADICIONAR", 5));

        votoConsumer.flush();

        ArgumentCaptor<PrioridadeEscaladaMessage> captor = ArgumentCaptor.forClass(PrioridadeEscaladaMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitWorkerConfig.EXCHANGE_NAME),
                eq(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_PRIORIDADE_ESCALADA),
                captor.capture()
        );
        assertEquals(PrioridadeChamado.URGENTE, captor.getValue().novaPrioridade());
    }

    @Test
    @DisplayName("Não deve publicar escalonamento quando saldo da janela for negativo (REMOVER)")
    void naoDeveEscalarQuandoOperacaoForRemover() {
        votoConsumer.consumeVoto(new VotoMessage(20L, 1L, "REMOVER", 2));

        votoConsumer.flush();

        verify(rabbitTemplate, never()).convertAndSend(any(), any(), any(Object.class));
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }
}
