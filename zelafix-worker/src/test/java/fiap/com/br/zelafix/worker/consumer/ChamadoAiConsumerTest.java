package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.config.RabbitWorkerConfig;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import fiap.com.br.zelafix.worker.queue.dto.TriagemFeedbackMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(
                100L,
                "Cratera perigosa",
                "Buraco profundo na via",
                "Pavimentação",
                "Pinheiros",
                fotoBytes
        );

        TriagemIaResult resultado = new TriagemIaResult(
                PrioridadeChamado.ALTA,
                DepartamentoGoverno.SECRETARIA_OBRAS,
                "Vistoria emergencial recomendada para recapeamento."
        );

        when(aiService.analisarChamado("Pavimentação", "Cratera perigosa", "Buraco profundo na via", "Pinheiros", fotoBytes))
                .thenReturn(resultado);

        consumer.processarTriagem(message);

        verify(aiService).analisarChamado("Pavimentação", "Cratera perigosa", "Buraco profundo na via", "Pinheiros", fotoBytes);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitWorkerConfig.EXCHANGE_NAME),
                eq(RabbitWorkerConfig.ROUTING_KEY_CHAMADO_TRIAGEM_CONCLUIDA),
                any(TriagemFeedbackMessage.class)
        );
    }
}
