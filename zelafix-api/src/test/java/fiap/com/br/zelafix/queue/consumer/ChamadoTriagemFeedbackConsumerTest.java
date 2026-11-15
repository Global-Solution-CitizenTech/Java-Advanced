package fiap.com.br.zelafix.queue.consumer;

import fiap.com.br.zelafix.categoria.CategoriaEnum;
import fiap.com.br.zelafix.chamado.ChamadoService;
import fiap.com.br.zelafix.chamado.TriagemPendenteStore;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.orgao.OrgaoGovernoEnum;
import fiap.com.br.zelafix.chamado.prioridade.PrioridadeEnum;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.exception.TriagemIndisponivelException;
import fiap.com.br.zelafix.historico.status.StatusChamadoEnum;
import fiap.com.br.zelafix.queue.dto.TriagemFeedbackMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        var message = TriagemFeedbackMessage.builder()
                .correlationId("corr-1")
                .usuarioId(1L)
                .enderecoId(2L)
                .foto(new byte[]{1, 2, 3})
                .titulo("Cratera aberta na avenida")
                .descricao("Afundamento asfáltico profundo na faixa direita oferecendo risco iminente.")
                .categoria(CategoriaEnum.VIAS_E_PAVIMENTACAO)
                .prioridade(PrioridadeEnum.URGENTE)
                .orgao(OrgaoGovernoEnum.SECRETARIA_OBRAS)
                .status(StatusChamadoEnum.EM_ANALISE)
                .observacoes("Triagem automática por IA concluída.")
                .build();
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
        var message = TriagemFeedbackMessage.builder()
                .correlationId("corr-2")
                .usuarioId(1L)
                .enderecoId(2L)
                .titulo("Imagem fora do escopo de zeladoria urbana")
                .descricao("A foto enviada é uma captura de tela de jogo e não mostra ocorrência urbana.")
                .recusado(true)
                .build();

        feedbackConsumer.receberFeedbackTriagem(message);

        verify(triagemPendenteStore).rejeitar("corr-2", message.descricao());
        verifyNoInteractions(chamadoService);
    }

    @Test
    @DisplayName("Deve falhar DeferredResult imediatamente quando worker reportar erro ou persistência falhar")
    void deveFalharImediatamenteQuandoErroOuExcecaoNaPersistencia() {
        var comErro = TriagemFeedbackMessage.builder()
                .correlationId("corr-3")
                .usuarioId(1L)
                .enderecoId(2L)
                .erro("Worker indisponível")
                .build();
        feedbackConsumer.receberFeedbackTriagem(comErro);
        verify(triagemPendenteStore).falhar(eq("corr-3"), any(TriagemIndisponivelException.class));
        verifyNoInteractions(chamadoService);

        var valido = TriagemFeedbackMessage.builder()
                .correlationId("corr-4")
                .usuarioId(1L)
                .enderecoId(2L)
                .foto(new byte[]{1})
                .titulo("Título")
                .descricao("Desc")
                .categoria(CategoriaEnum.LIMPEZA_URBANA)
                .prioridade(PrioridadeEnum.MEDIA)
                .orgao(OrgaoGovernoEnum.SUBPREFEITURA)
                .status(StatusChamadoEnum.PENDENTE)
                .observacoes("Aguardando triagem")
                .build();
        RuntimeException falhaBanco = new RuntimeException("ORA-12899");
        when(chamadoService.aplicarTriagemIa(valido)).thenThrow(falhaBanco);

        feedbackConsumer.receberFeedbackTriagem(valido);
        verify(triagemPendenteStore).falhar("corr-4", falhaBanco);
    }
}
