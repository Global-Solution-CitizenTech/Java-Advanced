package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.dto.TriagemPreAnaliseRequest;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TriagemAiControllerTest {

    @Mock
    private ZelaFixAiService aiService;

    @InjectMocks
    private TriagemAiController controller;

    @Test
    @DisplayName("Deve decodificar Base64 e retornar categoria, prioridade e parecer")
    void devePreAnalisarFotoComSucesso() {
        byte[] fakeJpg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        var req = new TriagemPreAnaliseRequest("Poste caído", "Fio solto na calçada", "Centro", Base64.getEncoder().encodeToString(fakeJpg));

        when(aiService.analisarChamado(eq("A definir pela IA"), any(), any(), any(), any())).thenReturn(
                new TriagemIaResult(
                        CategoriaSugerida.ILUMINACAO_PUBLICA,
                        PrioridadeChamado.URGENTE,
                        DepartamentoGoverno.ILUMINACAO_PUBLICA,
                        "Risco de choque elétrico imediato."
                )
        );

        Map<String, String> res = controller.preAnalisar(req);

        assertEquals("Iluminação Pública", res.get("categoriaNome"));
        assertEquals("URGENTE", res.get("prioridade"));
        assertEquals("Risco de choque elétrico imediato.", res.get("parecerIa"));
    }
}
