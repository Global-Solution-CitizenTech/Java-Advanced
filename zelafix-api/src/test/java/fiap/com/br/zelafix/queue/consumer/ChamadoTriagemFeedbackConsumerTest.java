package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.chamado.TriagemPendenteStore;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoTriagemFeedbackConsumerTest {

    @Mock
    private ChamadoService chamadoService;
    @Mock
    private TriagemPendenteStore triagemPendenteStore;

    @InjectMocks
    private ChamadoTriagemFeedbackConsumer feedbackConsumer;

    @Test
    @DisplayName("Deve delegar a triagem da IA para persistir o chamado e concluir o DeferredResult")
    void deveDelegarFeedbackParaService() {
        var message = new TriagemFeedbackMessage(
                "corr-1",
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
        var response = new ChamadoResponse(
                10L,
                "Cratera aberta na avenida",
                "Afundamento asfáltico profundo na faixa direita oferecendo risco iminente.",
                LocalDateTime.now(),
                2L,
                "VIAS_E_PAVIMENTACAO",
                4L,
                "URGENTE",
                1L,
                "SECRETARIA_OBRAS",
                1L,
                "Cidadão",
                new EnderecoResponse(2L, "01001-000", "100", "Rua Principal", "Ref", "Centro", "São Paulo", "SP"),
                "/chamados/10/foto",
                "EM_ANALISE",
                1L,
                true,
                List.of()
        );
        when(chamadoService.aplicarTriagemIa(message)).thenReturn(response);

        feedbackConsumer.receberFeedbackTriagem(message);

        verify(chamadoService).aplicarTriagemIa(message);
        verify(triagemPendenteStore).concluir("corr-1", response);
    }

    @Test
    @DisplayName("Deve rejeitar a triagem pendente sem persistir o chamado quando recusado for true")
    void deveRegistrarRecusaQuandoImagemInvalida() {
        var message = new TriagemFeedbackMessage(
                "corr-2",
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

        verify(triagemPendenteStore).rejeitar("corr-2", message.descricao());
        verifyNoInteractions(chamadoService);
    }
}
