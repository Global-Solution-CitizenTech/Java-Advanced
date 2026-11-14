package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriagemNormativaToolsTest {

    private final TriagemNormativaTools tools = new TriagemNormativaTools();

    @Test
    @DisplayName("Deve retornar decreto municipal e órgão padrão para categoria de zeladoria")
    void deveConsultarNormaMunicipal() {
        var regra = tools.consultarNormaMunicipal(CategoriaSugerida.DRENAGEM_E_ESGOTO);

        assertEquals(DepartamentoGoverno.SANEAMENTO_BASICO, regra.orgaoPadrao());
        assertTrue(regra.decreto().contains("57.545/2016"));
    }

    @Test
    @DisplayName("Deve calcular prazo SLA em horas para a prioridade informada")
    void deveCalcularPrazoSla() {
        String resposta = tools.calcularPrazoSla(PrioridadeChamado.URGENTE);

        assertTrue(resposta.contains("URGENTE: 12h"));
        assertTrue(resposta.contains("America/Sao_Paulo"));
    }
}
