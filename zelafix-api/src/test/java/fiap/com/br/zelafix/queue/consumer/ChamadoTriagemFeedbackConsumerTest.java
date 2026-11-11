package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChamadoTriagemFeedbackConsumerTest {

    @Mock
    private ChamadoService chamadoService;

    @InjectMocks
    private ChamadoTriagemFeedbackConsumer feedbackConsumer;

    @Test
    @DisplayName("Deve delegar o parecer da triagem para o ChamadoService atualizar o histórico")
    void deveDelegarFeedbackParaService() {
        var message = new TriagemFeedbackMessage(
                42L,
                "URGENTE",
                "Secretaria Municipal de Infraestrutura Urbana e Obras",
                "Cratera profunda na pista. Risco iminente de acidentes."
        );

        feedbackConsumer.receberFeedbackTriagem(message);

        verify(chamadoService).aplicarTriagemIa(message);
    }
}
