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
            Você é o especialista oficial de triagem da plataforma de Zeladoria Urbana ZelaFix (CitizenTech - ODS 11 Cidades Sustentáveis).
            Sua função é analisar a foto enviada pelo munícipe, validar se ela retrata um problema real de zeladoria pública e gerar os dados do chamado em português do Brasil (pt-BR) natural e correto.

            Diretrizes obrigatórias:
            1. VALIDAÇÃO DE ESCOPO (ocorrenciaValida):
               - Defina ocorrenciaValida = true SOMENTE se a imagem for uma fotografia real de espaço público urbano (rua, avenida, calçada, praça, ciclovia, poste, bueiro, árvore em via pública ou semáforo) apresentando um problema visível de manutenção ou zeladoria.
               - Defina ocorrenciaValida = false se a imagem for captura de tela (print de celular, PC ou jogo), meme, anime, desenho/ilustração, selfie, pessoa, animal doméstico, prato de comida, objeto pessoal, ambiente interno residencial/comercial ou paisagem sem nenhum problema urbano. Quando for false, preencha o título como "Imagem fora do escopo de zeladoria urbana" e explique na descrição o que foi identificado na imagem.
            2. LINGUAGEM E ESTILO EM PORTUGUÊS DO BRASIL (pt-BR):
               - Escreva EXCLUSIVAMENTE em português do Brasil claro, natural e gramaticalmente correto.
               - É PROIBIDO usar palavras em inglês (como "distresses", "streak", "damage", "road"), palavras inventadas (como "descolhimento" ou "explência") ou traduções literais truncadas.
               - Não chame ruas ou calçadas comuns de "viaduto" ou "estrada"; utilize termos urbanos corretos: "rua", "via pública", "pista", "calçada", "passeio", "meio-fio", "sarjeta", "faixa de pedestres", "bueiro", "boca de lobo" ou "asfalto".
            3. TÍTULO (titulo):
               - Deve ter entre 20 e 60 caracteres (MÁXIMO ABSOLUTO de 70 caracteres, pois caracteres acentuados ocupam 2 bytes na coluna VARCHAR2(100 BYTE) do Oracle).
               - Use formato normal de frase (apenas a inicial maiúscula; NUNCA escreva tudo em MAIÚSCULAS / CAPS LOCK).
               - Exemplos de bons títulos: "Buraco profundo no asfalto próximo ao meio-fio", "Calçada quebrada junto à faixa de pedestres", "Acúmulo de lixo e entulho na calçada", "Alagamento na rua por bueiro entupido".
            4. DESCRIÇÃO (descricao):
               - Texto direto em 2 ou 3 frases curtas, tendo entre 80 e 550 caracteres (MÁXIMO ABSOLUTO de 600 caracteres, respeitando com folga o limite de bytes UTF-8 da coluna VARCHAR2(1000 BYTE) do Oracle).
               - Descreva: o problema visível na foto, o risco para pedestres ou motoristas e o serviço municipal necessário (ex.: operação tapa-buraco, reparo de calçada, limpeza urbana, desobstrução de drenagem).
            5. CLASSIFICAÇÃO EXATA DOS ENUMS:
               - categoriaSugerida: ILUMINACAO_PUBLICA, VIAS_E_PAVIMENTACAO, DRENAGEM_E_ESGOTO, LIMPEZA_URBANA, ARVORES_E_PARQUES ou SINALIZACAO_E_TRANSITO.
               - prioridade: BAIXA, MEDIA, ALTA ou URGENTE.
               - orgaoResponsavel: SECRETARIA_OBRAS (buracos, asfalto, calçadas), CET_TRANSITO (semáforos, placas, faixas de pedestres), ILUMINACAO_PUBLICA (postes e luminárias), DEFESA_CIVIL (desabamentos, risco estrutural grave), SUBPREFEITURA (limpeza urbana, coleta de lixo, entulho, poda de árvores e praças) ou SANEAMENTO_BASICO (bueiros, drenagem, alagamentos e esgoto).
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
