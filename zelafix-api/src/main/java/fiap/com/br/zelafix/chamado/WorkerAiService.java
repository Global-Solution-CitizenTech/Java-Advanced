package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoTriagemPreviewResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoTriagemRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(
        url = "${zelafix.worker.url:http://localhost:8081}",
        accept = "application/json",
        contentType = "application/json"
)
public interface WorkerAiService {

    @PostExchange("/ai/triagem")
    ChamadoTriagemPreviewResponse preAnalisar(@RequestBody ChamadoTriagemRequest request);
}
