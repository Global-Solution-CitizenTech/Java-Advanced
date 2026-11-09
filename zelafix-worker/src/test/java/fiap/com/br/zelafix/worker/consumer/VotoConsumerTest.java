package fiap.com.br.zelafix.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class VotoConsumerTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VotoConsumer votoConsumer;

    @Test
    @DisplayName("Deve acumular votos no buffer em memória e realizar flush")
    void deveAcumularVotosERealizarFlush() {
        VotoMessage msg1 = new VotoMessage(10L, 1L);
        VotoMessage msg2 = new VotoMessage(10L, 2L);

        votoConsumer.consumeVoto(msg1);
        votoConsumer.consumeVoto(msg2);

        assertEquals(2, votoConsumer.getVotesBuffer().get(10L));

        votoConsumer.flush();

        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }

    @Test
    @DisplayName("Deve decrementar votos no buffer quando operacao for REMOVER")
    void deveDecrementarVotoQuandoOperacaoForRemover() {
        VotoMessage msgRemover = new VotoMessage(20L, 1L, "REMOVER");

        votoConsumer.consumeVoto(msgRemover);

        assertEquals(-1, votoConsumer.getVotesBuffer().get(20L));

        votoConsumer.flush();

        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }
}
