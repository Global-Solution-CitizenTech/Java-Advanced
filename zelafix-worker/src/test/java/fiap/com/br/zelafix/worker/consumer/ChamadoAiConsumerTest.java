package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.worker.queue.dto.TriagemFeedbackMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoAiConsumerTest {

    @Mock
    private ZelaFixAiService aiService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ChamadoAiConsumer consumer;

    @Test
    @DisplayName("Deve processar triagem multimodal assíncrona enviando os dados e a foto para o Spring AI")
    void deveProcessarTriagem() {
        byte[] fotoBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(1L, 10L, "Pinheiros", fotoBytes);

        TriagemIaResult resultado = new TriagemIaResult(
                true,
                "Cratera perigosa na via",
                "Buraco profundo no pavimento asfáltico oferecendo risco aos motoristas.",
                CategoriaSugerida.VIAS_E_PAVIMENTACAO,
                PrioridadeChamado.ALTA,
                DepartamentoGoverno.SECRETARIA_OBRAS
        );

        when(aiService.analisarChamado("Pinheiros", fotoBytes)).thenReturn(resultado);

        consumer.processarTriagem(message);

        verify(aiService).analisarChamado("Pinheiros", fotoBytes);
        ArgumentCaptor<TriagemFeedbackMessage> captor = ArgumentCaptor.forClass(TriagemFeedbackMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitWorkerConfig.EXCHANGE_NAME),
                eq(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA),
                captor.capture()
        );
        assertEquals("EM_ANALISE", captor.getValue().statusNome());
        assertEquals("VIAS_E_PAVIMENTACAO", captor.getValue().categoriaNome());
        assertEquals("ALTA", captor.getValue().prioridadeNome());
        assertEquals("SECRETARIA_OBRAS", captor.getValue().orgaoNome());
    }

    @Test
    @DisplayName("Deve recusar e descartar chamado sem publicar na fila quando a imagem for inválida")
    void deveRecusarChamadoQuandoImagemInvalida() {
        byte[] fotoBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(1L, 10L, "Pinheiros", fotoBytes);

        TriagemIaResult resultadoInvalido = new TriagemIaResult(
                false,
                "Imagem fora do escopo de zeladoria urbana",
                "A imagem enviada é uma captura de tela de jogo e não retrata um problema urbano.",
                CategoriaSugerida.LIMPEZA_URBANA,
                PrioridadeChamado.BAIXA,
                DepartamentoGoverno.SUBPREFEITURA
        );

        when(aiService.analisarChamado("Pinheiros", fotoBytes)).thenReturn(resultadoInvalido);

        consumer.processarTriagem(message);

        verify(aiService).analisarChamado("Pinheiros", fotoBytes);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    @DisplayName("Deve publicar feedback de contingência com status PENDENTE quando a IA falhar")
    void deveAplicarContingenciaQuandoIaFalhar() {
        byte[] fotoBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(1L, 10L, "Pinheiros", fotoBytes);

        when(aiService.analisarChamado("Pinheiros", fotoBytes)).thenThrow(new RuntimeException("Azure timeout"));

        consumer.processarTriagem(message);

        ArgumentCaptor<TriagemFeedbackMessage> captor = ArgumentCaptor.forClass(TriagemFeedbackMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitWorkerConfig.EXCHANGE_NAME),
                eq(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA),
                captor.capture()
        );
        assertEquals("PENDENTE", captor.getValue().statusNome());
        assertEquals("VIAS_E_PAVIMENTACAO", captor.getValue().categoriaNome());
        assertEquals("MEDIA", captor.getValue().prioridadeNome());
        assertEquals("SUBPREFEITURA", captor.getValue().orgaoNome());
    }
}
