package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.enums.CategoriaSugerida;
import fiap.com.br.zelafix.worker.ai.enums.DepartamentoGoverno;
import fiap.com.br.zelafix.worker.ai.enums.PrioridadeChamado;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Ferramentas Spring AI ({@code @Tool}) para consulta stateless de competência municipal e SLA na triagem.
 */
@Component
public class TriagemNormativaTools {

    private static final ZoneId ZONE_SP = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public record RegraMunicipal(String decreto, DepartamentoGoverno orgaoPadrao, String criterio) {}

    private static final Map<CategoriaSugerida, RegraMunicipal> REGRAS = Map.of(
            CategoriaSugerida.VIAS_E_PAVIMENTACAO, new RegraMunicipal(
                    "Decreto PMSP nº 59.150/2019", DepartamentoGoverno.SECRETARIA_OBRAS,
                    "Buracos, afundamentos e calçadas danificadas; risco estrutural grave vai para DEFESA_CIVIL."),
            CategoriaSugerida.DRENAGEM_E_ESGOTO, new RegraMunicipal(
                    "Decreto PMSP nº 57.545/2016", DepartamentoGoverno.SANEAMENTO_BASICO,
                    "Bueiros entupidos, bocas de lobo sem tampa, alagamentos e vazamentos."),
            CategoriaSugerida.ILUMINACAO_PUBLICA, new RegraMunicipal(
                    "Portaria SGM/SES nº 03/2021", DepartamentoGoverno.ILUMINACAO_PUBLICA,
                    "Postes apagados, luminárias queimadas ou fiação de iluminação pública."),
            CategoriaSugerida.LIMPEZA_URBANA, new RegraMunicipal(
                    "Lei Municipal nº 13.478/2002", DepartamentoGoverno.SUBPREFEITURA,
                    "Acúmulo de lixo, entulho irregular e limpeza de vias públicas."),
            CategoriaSugerida.ARVORES_E_PARQUES, new RegraMunicipal(
                    "Lei Municipal nº 17.267/2020", DepartamentoGoverno.SUBPREFEITURA,
                    "Podas e praças competem à SUBPREFEITURA; queda com risco iminente vai para DEFESA_CIVIL."),
            CategoriaSugerida.SINALIZACAO_E_TRANSITO, new RegraMunicipal(
                    "Código de Trânsito Brasileiro", DepartamentoGoverno.CET_TRANSITO,
                    "Semáforos apagados/danificados, placas e faixas de pedestres.")
    );

    private static final Map<PrioridadeChamado, Integer> SLA_HORAS = Map.of(
            PrioridadeChamado.URGENTE, 12,
            PrioridadeChamado.ALTA, 24,
            PrioridadeChamado.MEDIA, 48,
            PrioridadeChamado.BAIXA, 72
    );

    @Tool(description = "Consulta o decreto municipal de São Paulo, órgão responsável oficial e critério de enquadramento da categoria")
    public RegraMunicipal consultarNormaMunicipal(@ToolParam(description = "Categoria urbana identificada na ocorrência") CategoriaSugerida categoria) {
        return REGRAS.get(categoria);
    }

    @Tool(description = "Retorna a data/hora atual em São Paulo e calcula o prazo limite (SLA) de atendimento para a prioridade")
    public String calcularPrazoSla(@ToolParam(description = "Nível de prioridade da ocorrência") PrioridadeChamado prioridade) {
        int horas = SLA_HORAS.get(prioridade);
        ZonedDateTime agora = ZonedDateTime.now(ZONE_SP);
        ZonedDateTime limite = agora.plusHours(horas);
        return "Registrado em %s (America/Sao_Paulo). SLA legal para prioridade %s: %dh (atendimento previsto até %s)."
                .formatted(agora.format(FORMATTER), prioridade, horas, limite.format(FORMATTER));
    }
}
