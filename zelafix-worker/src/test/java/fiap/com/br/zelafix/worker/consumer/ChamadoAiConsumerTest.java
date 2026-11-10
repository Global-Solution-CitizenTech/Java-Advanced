package fiap.com.br.zelafix.worker.consumer;

import fiap.com.br.zelafix.worker.ai.ZelaFixAiService;
import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import fiap.com.br.zelafix.worker.queue.dto.ChamadoTriagemMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChamadoAiConsumerTest {

    @Mock
    private ZelaFixAiService aiService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ChamadoAiConsumer consumer;

    @Test
    @DisplayName("Deve descartar com segurança quando a mensagem for nula")
    void deveDescartarMensagemNula() {
        assertDoesNotThrow(() -> consumer.processarTriagem(null));
        verify(aiService, never()).analisarChamado(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve descartar com segurança quando idChamado for nulo")
    void deveDescartarMensagemComIdNulo() {
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(null, "Título", "Desc", "Cat", "Bairro");
        assertDoesNotThrow(() -> consumer.processarTriagem(message));
        verify(aiService, never()).analisarChamado(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve processar triagem assíncrona, chamar a IA e atualizar o histórico no banco")
    void deveProcessarTriagemEAtualizarHistorico() {
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(
                100L,
                "Cratera perigosa",
                "Buraco profundo na via",
                "Pavimentação",
                "Pinheiros"
        );

        byte[] fotoSimulada = new byte[]{1, 2, 3};
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<byte[]>>any(), eq(100L)))
                .thenReturn(fotoSimulada);

        TriagemIaResult resultado = new TriagemIaResult(
                PrioridadeChamado.ALTA,
                DepartamentoGoverno.SECRETARIA_OBRAS,
                "Vistoria emergencial recomendada para recapeamento."
        );

        when(aiService.analisarChamado(
                eq("Pavimentação"),
                eq("Cratera perigosa"),
                eq("Buraco profundo na via"),
                eq("Pinheiros"),
                eq(fotoSimulada)
        )).thenReturn(resultado);

        consumer.processarTriagem(message);

        verify(jdbcTemplate).update(
                anyString(),
                eq("[Triagem IA - ALTA | Secretaria Municipal de Infraestrutura Urbana e Obras] Vistoria emergencial recomendada para recapeamento."),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Deve prosseguir com foto nula se consulta ao banco lançar exceção")
    void deveTratarExcecaoAoBuscarFoto() {
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(
                101L,
                "Luz apagada",
                "Poste sem luz",
                "Iluminação",
                "Moema"
        );

        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<byte[]>>any(), eq(101L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        TriagemIaResult resultado = new TriagemIaResult(
                PrioridadeChamado.MEDIA,
                DepartamentoGoverno.ILUMINACAO_PUBLICA,
                "Troca de lâmpada necessária."
        );

        when(aiService.analisarChamado(
                eq("Iluminação"),
                eq("Luz apagada"),
                eq("Poste sem luz"),
                eq("Moema"),
                isNull()
        )).thenReturn(resultado);

        consumer.processarTriagem(message);

        verify(jdbcTemplate).update(
                anyString(),
                eq("[Triagem IA - MEDIA | Departamento de Iluminação Pública (Ilume)] Troca de lâmpada necessária."),
                eq(101L)
        );
    }

    @Test
    @DisplayName("Deve truncar observação para 255 caracteres caso o parecer da IA exceda o limite")
    void deveTruncarObservacaoMaiorQue255Caracteres() {
        ChamadoTriagemMessage message = new ChamadoTriagemMessage(
                102L,
                "Vazamento grave",
                "Água jorrando na calçada",
                "Saneamento",
                "Itaim Bibi"
        );

        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.<RowMapper<byte[]>>any(), eq(102L)))
                .thenReturn(null);

        String textoGigante = "A".repeat(300);
        TriagemIaResult resultado = new TriagemIaResult(
                PrioridadeChamado.URGENTE,
                DepartamentoGoverno.SANEAMENTO_BASICO,
                textoGigante
        );

        when(aiService.analisarChamado(any(), any(), any(), any(), any())).thenReturn(resultado);

        consumer.processarTriagem(message);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).update(anyString(), captor.capture(), eq(102L));

        String observacaoGravada = captor.getValue();
        assertEquals(255, observacaoGravada.length());
        assertTrue(observacaoGravada.endsWith("..."));
    }
}
