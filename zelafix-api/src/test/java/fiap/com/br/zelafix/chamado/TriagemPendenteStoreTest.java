package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriagemPendenteStoreTest {

    private final TriagemPendenteStore store = new TriagemPendenteStore();

    @Test
    @DisplayName("Deve resolver DeferredResult pendente quando triagem for concluída")
    void deveConcluirTriagemPendente() {
        AtomicReference<String> correlationIdRef = new AtomicReference<>();
        DeferredResult<ChamadoResponse> deferred = store.registrar(correlationId -> {
            assertNotNull(correlationId);
            assertFalse(correlationId.isBlank());
            correlationIdRef.set(correlationId);
        });

        ChamadoResponse esperado = criarResponseExemplo();
        store.concluir(correlationIdRef.get(), esperado);

        assertTrue(deferred.hasResult());
        assertEquals(esperado, deferred.getResult());
    }

    @Test
    @DisplayName("Deve completar DeferredResult com IllegalArgumentException quando triagem for rejeitada")
    void deveRejeitarTriagemPendente() {
        AtomicReference<String> correlationIdRef = new AtomicReference<>();
        DeferredResult<ChamadoResponse> deferred = store.registrar(correlationIdRef::set);

        store.rejeitar(correlationIdRef.get(), "A foto não retrata problema urbano.");

        assertTrue(deferred.hasResult());
        IllegalArgumentException erro = assertInstanceOf(IllegalArgumentException.class, deferred.getResult());
        assertEquals("A foto não retrata problema urbano.", erro.getMessage());
    }

    @Test
    @DisplayName("Deve remover pendência e propagar erro quando publicação falhar")
    void deveLimparPendenciaQuandoPublicadorFalhar() {
        AtomicReference<String> correlationIdRef = new AtomicReference<>();

        assertThrows(AmqpException.class, () -> store.registrar(correlationId -> {
            correlationIdRef.set(correlationId);
            throw new AmqpException("Broker offline");
        }));

        store.concluir(correlationIdRef.get(), criarResponseExemplo());
    }

    @Test
    @DisplayName("Deve ignorar conclusão tardia quando correlationId não estiver mais pendente")
    void deveIgnorarConclusaoQuandoNaoPendente() {
        store.concluir("inexistente", criarResponseExemplo());
        store.rejeitar("inexistente", "Motivo");
    }

    private ChamadoResponse criarResponseExemplo() {
        return new ChamadoResponse(
                101L,
                "Buraco no asfalto",
                "Buraco fundo na pista",
                LocalDateTime.now(),
                1L,
                "VIAS_E_PAVIMENTACAO",
                2L,
                "MEDIA",
                5L,
                "SUBPREFEITURA",
                1L,
                "Carlos Munícipe",
                new EnderecoResponse(1L, "05422-001", "720", "Rua dos Pinheiros", "Esquina", "Pinheiros", "São Paulo", "SP"),
                "/chamados/101/foto",
                "EM_ANALISE",
                1L,
                true,
                List.of()
        );
    }
}
