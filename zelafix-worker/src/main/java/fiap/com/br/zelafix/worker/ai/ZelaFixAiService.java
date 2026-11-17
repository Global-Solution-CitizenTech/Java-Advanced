package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.util.ImageMimeTypeDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

/**
 * Serviço de Inteligência Artificial Generativa e Multimodal com Spring AI.
 *
 * Recursos utilizados:
 * - Injeção idiomática de {@link ChatClient.Builder} com {@link SimpleLoggerAdvisor}.
 * - Tool Calling declarativo ({@link TriagemNormativaTools}) para normas municipais e SLA.
 * - Visão multimodal com detecção de MIME type e prompt parametrizado.
 * - Structured Output validado com {@code EntityParamSpec::validateSchema} e limite UTF-8 Oracle.
 * - Estratégia Fail-Fast imediata delegando contingência graciosa ao {@code ChamadoAiConsumer}.
 */
@Slf4j
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
            2. CONSULTA DE NORMAS E SLA (TOOL CALLING):
               - Quando ocorrenciaValida = true, utilize consultarNormaMunicipal para confirmar decreto e órgão competente da categoria e calcularPrazoSla para obter o prazo limite da prioridade.
               - Quando ocorrenciaValida = false, NÃO chame nenhuma ferramenta e responda diretamente.
            3. LINGUAGEM E ESTILO EM PORTUGUÊS DO BRASIL (pt-BR):
               - Escreva EXCLUSIVAMENTE em português do Brasil claro, natural e gramaticalmente correto.
               - É PROIBIDO usar palavras em inglês (como "distresses", "streak", "damage", "road"), palavras inventadas (como "descolhimento" ou "explência") ou traduções literais truncadas.
               - Não chame ruas ou calçadas comuns de "viaduto" ou "estrada"; utilize termos urbanos corretos: "rua", "via pública", "pista", "calçada", "passeio", "meio-fio", "sarjeta", "faixa de pedestres", "bueiro", "boca de lobo" ou "asfalto".
            4. TÍTULO (titulo):
               - Frase curta, direta e objetiva (sem usar tudo em MAIÚSCULAS / CAPS LOCK).
               - Exemplos de bons títulos: "Buraco profundo no asfalto próximo ao meio-fio", "Calçada quebrada junto à faixa de pedestres", "Acúmulo de lixo e entulho na calçada", "Alagamento na rua por bueiro entupido".
            5. DESCRIÇÃO (descricao):
               - Texto direto em 2 ou 3 frases curtas e objetivas.
               - Descreva: o problema visível na foto, o risco para pedestres ou motoristas e o serviço municipal necessário (ex.: operação tapa-buraco, reparo de calçada, limpeza urbana, desobstrução de drenagem).
            6. CLASSIFICAÇÃO EXATA DOS ENUMS:
               - categoriaSugerida: ILUMINACAO_PUBLICA, VIAS_E_PAVIMENTACAO, DRENAGEM_E_ESGOTO, LIMPEZA_URBANA, ARVORES_E_PARQUES ou SINALIZACAO_E_TRANSITO.
               - prioridade: BAIXA, MEDIA, ALTA ou URGENTE.
               - orgaoResponsavel: SECRETARIA_OBRAS (buracos, asfalto, calçadas), CET_TRANSITO (semáforos, placas, faixas de pedestres), ILUMINACAO_PUBLICA (postes e luminárias), DEFESA_CIVIL (desabamentos, risco estrutural grave), SUBPREFEITURA (limpeza urbana, coleta de lixo, entulho, poda de árvores e praças) ou SANEAMENTO_BASICO (bueiros, drenagem, alagamentos e esgoto).
            """;

    public ZelaFixAiService(ChatClient.Builder builder, TriagemNormativaTools triagemTools) {
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(triagemTools)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    public TriagemIaResult analisarChamado(String bairro, byte[] fotoBytes) {
        log.info("Invocando endpoint de IA para bairro: '{}' (tamanho da foto: {} bytes)", bairro, fotoBytes.length);
        long start = System.currentTimeMillis();
        byte[] imagemParaIa = ImageMimeTypeDetector.redimensionarParaIa(fotoBytes);
        TriagemIaResult resultado = chatClient.prompt()
                .user(u -> {
                    u.text("Analise a foto desta ocorrência urbana registrada no bairro {bairro} e gere os dados completos do chamado.");
                    u.param("bairro", bairro);
                    MimeType mimeType = ImageMimeTypeDetector.detectar(imagemParaIa);
                    u.media(mimeType, new ByteArrayResource(imagemParaIa));
                })
                .call()
                .entity(TriagemIaResult.class, ChatClient.EntityParamSpec::validateSchema)
                .comLimitesOracle();
        log.info("Retorno da IA recebido com sucesso em {}ms: [válida={}] {} (prioridade={}, órgão={})", System.currentTimeMillis() - start, resultado.ocorrenciaValida(), resultado.titulo(), resultado.prioridade(), resultado.orgaoResponsavel());
        return resultado;
    }
}
