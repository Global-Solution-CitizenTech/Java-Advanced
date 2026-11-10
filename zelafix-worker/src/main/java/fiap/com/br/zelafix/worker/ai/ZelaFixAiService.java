package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.util.ImageMimeTypeDetector;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

/**
 * Serviço de Inteligência Artificial Generativa e Multimodal com Spring AI.
 *
 * Implementado no padrão idiomático das aulas da FIAP:
 * - Injeção direta de ChatClient.Builder sem verificações defensivas de null.
 * - defaultSystem centralizado e SimpleLoggerAdvisor para rastreamento de prompts.
 * - Suporte a visão multimodal via MimeTypeUtils e ByteArrayResource.
 * - Structured Output com desserialização automática em BeanOutputConverter (.entity()).
 */
@Service
public class ZelaFixAiService {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = """
            Você é o assistente técnico oficial de triagem inteligente da plataforma de Zeladoria Urbana ZelaFix (CitizenTech - ODS 11 Cidades Sustentáveis).
            Sua função é analisar a ocorrência municipal (relato textual e foto anexa) e emitir a triagem técnica.

            Diretrizes:
            1. Analise cuidadosamente a imagem (se fornecida) e a descrição do munícipe para identificar a gravidade física.
            2. Classifique a prioridade rigorosamente de acordo com os valores permitidos (BAIXA, MEDIA, ALTA ou URGENTE).
            3. Selecione o departamento governamental municipal competente para atuar no reparo.
            4. Emita um parecer técnico objetivo e conciso (máximo 150 caracteres).
            """;

    public ZelaFixAiService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public TriagemIaResult analisarChamado(String categoria, String titulo, String descricao, String bairro) {
        return analisarChamado(categoria, titulo, descricao, bairro, null);
    }

    public TriagemIaResult analisarChamado(String categoria, String titulo, String descricao, String bairro, byte[] fotoBytes) {
        String userPrompt = """
                Dados da Ocorrência:
                - Categoria: %s
                - Bairro: %s
                - Título: %s
                - Descrição do Munícipe: %s
                """.formatted(categoria, bairro, titulo, descricao);

        return chatClient.prompt()
                .user(u -> {
                    u.text(userPrompt);
                    if (fotoBytes != null && fotoBytes.length > 0) {
                        MimeType mimeType = ImageMimeTypeDetector.detectar(fotoBytes);
                        u.media(mimeType, new ByteArrayResource(fotoBytes));
                    }
                })
                .call()
                .entity(TriagemIaResult.class);
    }
}
