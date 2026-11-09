package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ZelaFixAiServiceTest {

    private ZelaFixAiService aiService;

    @BeforeEach
    void setUp() {
        // Inicializa sem ChatClient (simulando ambiente offline / fallback gracioso)
        aiService = new ZelaFixAiService(null);
    }

    @Test
    @DisplayName("Deve classificar como URGENTE e Defesa Civil quando houver risco de desabamento ou alagamento")
    void deveClassificarComoUrgenteParaRiscoGrave() {
        TriagemIaResult resultado = aiService.analisarChamado(
                "Segurança Pública",
                "Risco iminente de desabamento de muro",
                "Muro de contenção rachou após chuva torrencial com perigo de queda",
                "Grajaú"
        );

        assertNotNull(resultado);
        assertEquals("URGENTE", resultado.prioridade());
        assertEquals("Coordenação Municipal de Defesa Civil", resultado.orgaoResponsavel());
    }

    @Test
    @DisplayName("Deve classificar como ALTA e Obras quando houver buraco ou problema de asfalto/esgoto")
    void deveClassificarComoAltaParaBuracoNaVia() {
        TriagemIaResult resultado = aiService.analisarChamado(
                "Pavimentação",
                "Buraco profundo na via principal",
                "Asfalto cedeu e está danificando veículos na avenida",
                "Pinheiros"
        );

        assertNotNull(resultado);
        assertEquals("ALTA", resultado.prioridade());
        assertEquals("Secretaria Municipal de Infraestrutura Urbana e Obras", resultado.orgaoResponsavel());
    }

    @Test
    @DisplayName("Deve classificar como MEDIA e Ilume quando houver poste ou lâmpada apagada")
    void deveClassificarComoMediaParaIluminacaoPublica() {
        TriagemIaResult resultado = aiService.analisarChamado(
                "Iluminação Pública",
                "Poste com luz piscando",
                "Lâmpada do poste em frente ao número 100 está queimada",
                "Moema"
        );

        assertNotNull(resultado);
        assertEquals("MEDIA", resultado.prioridade());
        assertEquals("Departamento de Iluminação Pública (Ilume)", resultado.orgaoResponsavel());
    }

    @Test
    @DisplayName("Deve classificar como BAIXA e Subprefeitura para ocorrências regulares de limpeza")
    void deveClassificarComoBaixaParaZeladoriaRegular() {
        TriagemIaResult resultado = aiService.analisarChamado(
                "Limpeza Comunitária",
                "Pintura de guia apagada",
                "A guia da calçada precisa de repintura",
                "Vila Mariana"
        );

        assertNotNull(resultado);
        assertEquals("BAIXA", resultado.prioridade());
        assertEquals("Subprefeitura Regional", resultado.orgaoResponsavel());
    }
}
