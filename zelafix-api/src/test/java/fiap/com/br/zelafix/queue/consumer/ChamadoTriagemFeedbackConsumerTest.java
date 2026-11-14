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
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ChamadoTriagemFeedbackConsumerTest {

    @Mock
    private ChamadoService chamadoService;

    @InjectMocks
    private ChamadoTriagemFeedbackConsumer feedbackConsumer;

    @Test
    @DisplayName("Deve delegar a triagem da IA para o ChamadoService persistir o chamado")
    void deveDelegarFeedbackParaService() {
        var message = new TriagemFeedbackMessage(
                1L,
                2L,
                new byte[]{1, 2, 3},
                "Cratera aberta na avenida",
                "Afundamento asfáltico profundo na faixa direita oferecendo risco iminente.",
                "VIAS_E_PAVIMENTACAO",
                "URGENTE",
                "SECRETARIA_OBRAS",
                "EM_ANALISE",
                false
        );

        feedbackConsumer.receberFeedbackTriagem(message);

        verify(chamadoService).aplicarTriagemIa(message);
    }

    @Test
    @DisplayName("Deve registrar recusa da IA sem persistir o chamado quando recusado for true")
    void deveRegistrarRecusaQuandoImagemInvalida() {
        var message = new TriagemFeedbackMessage(
                1L,
                2L,
                null,
                "Imagem fora do escopo de zeladoria urbana",
                "A foto enviada é uma captura de tela de jogo e não mostra ocorrência urbana.",
                null,
                null,
                null,
                null,
                true
        );

        feedbackConsumer.receberFeedbackTriagem(message);

        verify(chamadoService).registrarRecusaIa(1L, "A foto enviada é uma captura de tela de jogo e não mostra ocorrência urbana.");
        verifyNoMoreInteractions(chamadoService);
    }
}
