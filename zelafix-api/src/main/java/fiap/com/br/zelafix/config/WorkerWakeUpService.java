package fiap.com.br.zelafix.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Envia um ping HTTP assíncrono com cooldown de 5 minutos para acordar o worker no Azure Free Tier (F1).
 */
@Slf4j
@Service
public class WorkerWakeUpService {

    private static final long COOLDOWN_MS = Duration.ofMinutes(5).toMillis();

    private final String workerUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final AtomicLong lastWakeUpMs = new AtomicLong(0);

    public WorkerWakeUpService(@Value("${zelafix.worker.url:}") String workerUrl) {
        this.workerUrl = workerUrl == null ? "" : workerUrl.trim().replaceAll("/+$", "");
    }

    public void wakeUpAsync() {
        if (workerUrl.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        long prev = lastWakeUpMs.get();
        if (now - prev < COOLDOWN_MS || !lastWakeUpMs.compareAndSet(prev, now)) {
            return;
        }
        String healthUrl = workerUrl + "/actuator/health";
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ex) {
                log.debug("Wake-up assíncrono do worker ({}) falhou: {}", healthUrl, ex.getMessage());
            }
        });
    }
}
