package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Serviço de Inteligência Artificial Generativa com Spring AI (Rubrica 4 da Global Solution)
 *
 * Executa no microsserviço zelafix-worker para:
 * 1. Analisar a descrição do chamado e classificar a urgência (BAIXA, MEDIA, ALTA, URGENTE).
 * 2. Identificar o órgão municipal competente (Secretaria de Obras, CET/Trânsito, Iluminação, etc.).
 * 3. Emitir parecer técnico fundamentado para a equipe de zeladoria urbana da Prefeitura.
 * 4. Fallback heurístico resiliente para execução offline ou sem chave OpenAI.
 */
@Slf4j
@Service
public class ZelaFixAiService {

    private final ChatClient chatClient;

    public ZelaFixAiService(@Autowired(required = false) ChatClient.Builder chatClientBuilder) {
        if (chatClientBuilder != null) {
            ChatClient client = null;
            try {
                client = chatClientBuilder.build();
            } catch (Exception e) {
                log.warn(">>> [ZelaFix Worker] ChatClient.Builder indisponível. Usando fallback heurístico seguro.");
            }
            this.chatClient = client;
        } else {
            this.chatClient = null;
        }
    }

    public TriagemIaResult analisarChamado(String categoria, String titulo, String descricao, String bairro) {
        if (chatClient != null) {
            try {
                String prompt = """
                        Você é o assistente oficial de triagem inteligente da plataforma de Zeladoria Urbana ZelaFix (CitizenTech - ODS 11 Cidades Sustentáveis).
                        Analise a ocorrência municipal abaixo e responda no formato:
                        PRIORIDADE: [BAIXA | MEDIA | ALTA | URGENTE]
                        ORGAO: [Nome do órgão público municipal competente]
                        PARECER: [Breve parecer técnico explicando o risco e recomendando a ação da equipe municipal]

                        Ocorrência:
                        - Categoria: %s
                        - Bairro: %s
                        - Título: %s
                        - Descrição: %s
                        """.formatted(categoria, bairro, titulo, descricao);

                String content = chatClient.prompt().user(prompt).call().content();
                return parseiaResposta(content, categoria);
            } catch (Exception e) {
                log.warn(">>> [ZelaFix Worker] Erro ou ausência de cota OpenAI. Aplicando triagem heurística especializada: {}", e.getMessage());
            }
        }
        return aplicarHeuristicaTriagem(categoria, titulo, descricao);
    }

    private TriagemIaResult parseiaResposta(String content, String categoriaFallback) {
        if (content == null || content.isBlank()) {
            return aplicarHeuristicaTriagem(categoriaFallback, "", "");
        }

        String prioridade = "MEDIA";
        String orgao = "Zeladoria Municipal";
        String parecer = content;

        for (String linha : content.split("\n")) {
            String upper = linha.toUpperCase(Locale.ROOT);
            if (upper.startsWith("PRIORIDADE:")) {
                String p = upper.replace("PRIORIDADE:", "").trim();
                if (p.contains("URGENTE")) prioridade = "URGENTE";
                else if (p.contains("ALTA")) prioridade = "ALTA";
                else if (p.contains("BAIXA")) prioridade = "BAIXA";
                else prioridade = "MEDIA";
            } else if (upper.startsWith("ORGAO:") || upper.startsWith("ÓRGÃO:")) {
                orgao = linha.substring(linha.indexOf(':') + 1).trim();
            } else if (upper.startsWith("PARECER:")) {
                parecer = linha.substring(linha.indexOf(':') + 1).trim();
            }
        }

        return new TriagemIaResult(prioridade, orgao, parecer);
    }

    public TriagemIaResult aplicarHeuristicaTriagem(String categoria, String titulo, String descricao) {
        String texto = (categoria + " " + titulo + " " + descricao).toLowerCase(Locale.ROOT);

        String prioridade;
        String orgao;
        String parecer;

        if (texto.contains("risco") || texto.contains("cratera") || texto.contains("acidente") ||
                texto.contains("desabamento") || texto.contains("alagamento") || texto.contains("fogo")) {
            prioridade = "URGENTE";
            orgao = "Coordenação Municipal de Defesa Civil";
            parecer = "Triagem Inteligente: Risco iminente à segurança dos munícipes detectado no chamado. Despacho prioritário para vistoria emergencial em até 24h.";
        } else if (texto.contains("buraco") || texto.contains("asfalto") || texto.contains("vazamento") ||
                texto.contains("esgoto") || texto.contains("semáforo") || texto.contains("semaforo")) {
            prioridade = "ALTA";
            orgao = "Secretaria Municipal de Infraestrutura Urbana e Obras";
            parecer = "Triagem Inteligente: Impacto direto na mobilidade e circulação do bairro. Vistoria técnica recomendada para inclusão na rota semanal de reparos.";
        } else if (texto.contains("luz") || texto.contains("poste") || texto.contains("iluminação") ||
                texto.contains("iluminacao") || texto.contains("árvore") || texto.contains("arvore") || texto.contains("galho")) {
            prioridade = "MEDIA";
            orgao = "Departamento de Iluminação Pública (Ilume)";
            parecer = "Triagem Inteligente: Ocorrência de manutenção regular de infraestrutura urbana. Encaminhado para a equipe técnica responsável.";
        } else {
            prioridade = "BAIXA";
            orgao = "Subprefeitura Regional";
            parecer = "Triagem Inteligente: Ocorrência padrão de zeladoria e limpeza comunitária registrada com sucesso no sistema.";
        }

        return new TriagemIaResult(prioridade, orgao, parecer);
    }
}

