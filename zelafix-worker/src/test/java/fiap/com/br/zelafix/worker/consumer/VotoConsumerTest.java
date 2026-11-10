package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.queue.dto.VotoMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VotoConsumerTest {

    private VotoConsumer votoConsumer;

    @BeforeEach
    void setUp() {
        votoConsumer = new VotoConsumer();
    }

    @Test
    @DisplayName("Deve descartar com segurança quando a mensagem for nula")
    void deveDescartarMensagemNula() {
        assertDoesNotThrow(() -> votoConsumer.consumeVoto(null));
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }

    @Test
    @DisplayName("Deve descartar com segurança quando idChamado for nulo")
    void deveDescartarMensagemComIdChamadoNulo() {
        VotoMessage msg = new VotoMessage(null, 1L, "ADICIONAR");
        assertDoesNotThrow(() -> votoConsumer.consumeVoto(msg));
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }

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

    @Test
    @DisplayName("Deve executar flush sem erros quando o buffer estiver vazio")
    void deveExecutarFlushEmBufferVazio() {
        assertDoesNotThrow(() -> votoConsumer.flush());
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }

    @Test
    @DisplayName("Teste pesado de concorrência: 100 threads simultâneas acumulando votos com consistência")
    void testePesadoConcorrenciaVotos() throws InterruptedException {
        int numThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final long userId = i;
            executor.submit(() -> {
                try {
                    // Cada thread adiciona 2 votos para chamado 50 e 1 voto para chamado 60
                    votoConsumer.consumeVoto(new VotoMessage(50L, userId, "ADICIONAR"));
                    votoConsumer.consumeVoto(new VotoMessage(50L, userId, "ADICIONAR"));
                    votoConsumer.consumeVoto(new VotoMessage(60L, userId, "ADICIONAR"));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "As threads deveriam finalizar dentro do timeout");
        executor.shutdown();

        assertEquals(200, votoConsumer.getVotesBuffer().get(50L));
        assertEquals(100, votoConsumer.getVotesBuffer().get(60L));

        votoConsumer.flush();
        assertTrue(votoConsumer.getVotesBuffer().isEmpty());
    }
}
