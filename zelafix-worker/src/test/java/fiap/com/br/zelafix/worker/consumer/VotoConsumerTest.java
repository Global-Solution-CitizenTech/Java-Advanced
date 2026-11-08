package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.entity.ChamadoWorkerEntity;
import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import fiap.com.br.zelafix.worker.repository.ChamadoWorkerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VotoConsumerTest {

    @Mock
    private ChamadoWorkerRepository repository;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @InjectMocks
    private VotoConsumer votoConsumer;

    @Test
    @DisplayName("Deve acumular votos no buffer em memória e realizar flush para o banco de dados")
    void deveAcumularVotosERealizarFlush() {
        VotoMessage msg1 = new VotoMessage(10L, 1L);
        VotoMessage msg2 = new VotoMessage(10L, 2L);

        votoConsumer.consumeVoto(msg1);
        votoConsumer.consumeVoto(msg2);

        assertEquals(2, votoConsumer.getVotesBuffer().get(10L));

        ChamadoWorkerEntity entity = ChamadoWorkerEntity.builder()
                .idChamado(10L)
                .apoiosCount(5)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        votoConsumer.flush();

        assertEquals(7, entity.getApoiosCount());
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
        verify(repository, times(1)).save(entity);
    }

    @Test
    @DisplayName("Deve decrementar votos no buffer quando operacao for REMOVER")
    void deveDecrementarVotoQuandoOperacaoForRemover() {
        VotoMessage msgRemover = new VotoMessage(20L, 1L, "REMOVER");

        votoConsumer.consumeVoto(msgRemover);

        assertEquals(-1, votoConsumer.getVotesBuffer().get(20L));

        ChamadoWorkerEntity entity = ChamadoWorkerEntity.builder()
                .idChamado(20L)
                .apoiosCount(10)
                .build();

        when(repository.findById(20L)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenReturn(entity);

        votoConsumer.flush();

        assertEquals(9, entity.getApoiosCount());
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
        verify(repository, times(1)).save(entity);
    }
}

