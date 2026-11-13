package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.dto.TriagemPreAnaliseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/ai/triagem")
@RequiredArgsConstructor
public class TriagemAiController {

    private final ZelaFixAiService aiService;

    @PostMapping
    public Map<String, String> preAnalisar(@RequestBody TriagemPreAnaliseRequest request) {
        byte[] fotoBytes = Base64.getDecoder().decode(request.fotoBase64().trim());
        TriagemIaResult res = aiService.analisarChamado(
                "A definir pela IA",
                request.titulo(),
                request.descricao(),
                request.bairro(),
                fotoBytes
        );
        return Map.of(
                "categoriaNome", res.categoriaSugerida().getNomeCategoria(),
                "prioridade", res.prioridade().name(),
                "orgaoResponsavel", res.orgaoResponsavel().getNomeOficial(),
                "parecerIa", res.parecerIa()
        );
    }
}
