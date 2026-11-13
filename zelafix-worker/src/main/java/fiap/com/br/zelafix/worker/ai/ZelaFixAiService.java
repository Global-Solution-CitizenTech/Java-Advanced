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
            Sua função é analisar a imagem enviada pelo munícipe e gerar os dados técnicos completos do chamado.

            Diretrizes:
            1. Analise cuidadosamente a imagem para identificar o problema urbano, sua natureza e gravidade física.
            2. Gere um título curto, claro e objetivo (máximo 100 caracteres) que resuma o problema identificado na foto.
            3. Gere uma descrição técnica detalhada (máximo 1000 caracteres) explicando o problema observado, riscos aos cidadãos e recomendação de atendimento.
            4. Classifique a categoriaSugerida rigorosamente entre os valores permitidos (ILUMINACAO_PUBLICA, VIAS_E_PAVIMENTACAO, DRENAGEM_E_ESGOTO, LIMPEZA_URBANA, ARVORES_E_PARQUES ou SINALIZACAO_E_TRANSITO).
            5. Classifique a prioridade rigorosamente entre os valores permitidos (BAIXA, MEDIA, ALTA ou URGENTE).
            6. Selecione o departamento governamental municipal competente para atuar no reparo.
            """;

    public ZelaFixAiService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public TriagemIaResult analisarChamado(String bairro, byte[] fotoBytes) {
        String userPrompt = "Analise a foto desta ocorrência urbana registrada no bairro %s e gere os dados completos do chamado."
                .formatted(bairro);

        return chatClient.prompt()
                .user(u -> {
                    u.text(userPrompt);
                    MimeType mimeType = ImageMimeTypeDetector.detectar(fotoBytes);
                    u.media(mimeType, new ByteArrayResource(fotoBytes));
                })
                .call()
                .entity(TriagemIaResult.class);
    }
}
