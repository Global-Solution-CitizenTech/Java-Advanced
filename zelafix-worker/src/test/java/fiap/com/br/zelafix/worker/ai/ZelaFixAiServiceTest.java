package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZelaFixAiServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder builder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private ZelaFixAiService aiService;

    @BeforeEach
    void setUp() {
        when(builder.defaultSystem(anyString()).defaultAdvisors(any(Advisor[].class)).build()).thenReturn(chatClient);
        aiService = new ZelaFixAiService(builder);
    }

    @Test
    @DisplayName("Deve inicializar o ChatClient com o System Prompt e executar triagem tipada com Enums e imagem")
    void deveExecutarTriagemComSucesso() {
        TriagemIaResult esperado = new TriagemIaResult(
                "Muro rachado com risco de queda",
                "Estrutura de contenção apresenta fissuras graves próximas à via pública.",
                CategoriaSugerida.VIAS_E_PAVIMENTACAO,
                PrioridadeChamado.URGENTE,
                DepartamentoGoverno.DEFESA_CIVIL
        );

        when(chatClient.prompt().user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()).call().entity(TriagemIaResult.class))
                .thenReturn(esperado);

        TriagemIaResult resultado = aiService.analisarChamado(
                "Grajaú",
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0} // JPEG
        );

        assertNotNull(resultado);
        assertEquals("Muro rachado com risco de queda", resultado.titulo());
        assertEquals(PrioridadeChamado.URGENTE, resultado.prioridade());
        assertEquals(DepartamentoGoverno.DEFESA_CIVIL, resultado.orgaoResponsavel());
    }

    @Test
    @DisplayName("Deve processar foto PNG com detecção correta de MediaType")
    void deveProcessarFotoPng() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        TriagemIaResult esperado = new TriagemIaResult(
                "Bueiro entupido na via",
                "Boca de lobo obstruída por resíduos sólidos causando acúmulo de água.",
                CategoriaSugerida.DRENAGEM_E_ESGOTO,
                PrioridadeChamado.MEDIA,
                DepartamentoGoverno.SECRETARIA_OBRAS
        );

        when(chatClient.prompt().user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()).call().entity(TriagemIaResult.class))
                .thenReturn(esperado);

        TriagemIaResult resultado = aiService.analisarChamado("Santana", pngBytes);

        assertNotNull(resultado);
        assertEquals(PrioridadeChamado.MEDIA, resultado.prioridade());
    }

    @Test
    @DisplayName("Deve processar foto WebP do formato mobile")
    void deveProcessarFotoWebp() {
        byte[] webpBytes = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};
        TriagemIaResult esperado = new TriagemIaResult(
                "Semáforo apagado em cruzamento",
                "Conjunto semafórico sem alimentação elétrica aumentando o risco de colisões.",
                CategoriaSugerida.SINALIZACAO_E_TRANSITO,
                PrioridadeChamado.ALTA,
                DepartamentoGoverno.CET_TRANSITO
        );

        when(chatClient.prompt().user(ArgumentMatchers.<Consumer<ChatClient.PromptUserSpec>>any()).call().entity(TriagemIaResult.class))
                .thenReturn(esperado);

        TriagemIaResult resultado = aiService.analisarChamado("Consolação", webpBytes);

        assertNotNull(resultado);
        assertEquals(PrioridadeChamado.ALTA, resultado.prioridade());
        assertEquals(DepartamentoGoverno.CET_TRANSITO, resultado.orgaoResponsavel());
    }
}
