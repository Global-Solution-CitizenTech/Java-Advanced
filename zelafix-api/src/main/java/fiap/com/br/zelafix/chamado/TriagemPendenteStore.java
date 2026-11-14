package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Coordena requisições HTTP pendentes ({@link DeferredResult}) entre o envio para triagem no RabbitMQ
 * e o retorno assíncrono do worker de IA via {@code correlationId}.
 */
@Component
public class TriagemPendenteStore {

    private static final long TRIAGEM_TIMEOUT_MS = 60_000L;

    private final Map<String, DeferredResult<ChamadoResponse>> pendentes = new ConcurrentHashMap<>();

    public DeferredResult<ChamadoResponse> registrar(Consumer<String> publicador) {
        String correlationId = UUID.randomUUID().toString();
        DeferredResult<ChamadoResponse> result = new DeferredResult<>(TRIAGEM_TIMEOUT_MS);
        result.onCompletion(() -> pendentes.remove(correlationId));
        result.onTimeout(() -> pendentes.remove(correlationId));

        pendentes.put(correlationId, result);
        try {
            publicador.accept(correlationId);
        } catch (RuntimeException ex) {
            pendentes.remove(correlationId);
            throw ex;
        }
        return result;
    }

    public void concluir(String correlationId, ChamadoResponse chamado) {
        resolver(correlationId, pending -> pending.setResult(chamado));
    }

    public void rejeitar(String correlationId, String motivo) {
        resolver(correlationId, pending -> pending.setErrorResult(new IllegalArgumentException(motivo)));
    }

    private void resolver(String correlationId, Consumer<DeferredResult<ChamadoResponse>> acao) {
        Optional.ofNullable(pendentes.remove(correlationId)).ifPresent(acao);
    }
}
