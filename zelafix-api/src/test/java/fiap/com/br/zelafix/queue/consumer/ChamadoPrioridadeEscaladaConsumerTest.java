package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeEnum;
import fiap.com.br.zelafix.queue.dto.PrioridadeEscaladaMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChamadoPrioridadeEscaladaConsumerTest {

    @Mock
    private ChamadoService chamadoService;

    @InjectMocks
    private ChamadoPrioridadeEscaladaConsumer consumer;

    @Test
    @DisplayName("Deve delegar o evento de escalonamento para o ChamadoService")
    void deveDelegarEscalonamentoParaService() {
        var message = new PrioridadeEscaladaMessage(10L, PrioridadeEnum.ALTA, 2, "Atingiu 2 apoios.");

        consumer.receberEscalonamentoPrioridade(message);

        verify(chamadoService).escalarPrioridadePorApoio(message);
    }
}
