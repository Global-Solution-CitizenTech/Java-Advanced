package fiap.com.br.zelafix.worker.ai;

import fiap.com.br.zelafix.worker.ai.dto.TriagemIaResult;
import fiap.com.br.zelafix.worker.ai.util.ImageMimeTypeDetector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.ai.openai.base-url=${GROQ_BASE_URL:https://api.groq.com/openai}",
        "spring.ai.openai.api-key=${GROQ_API_KEY:test-key}",
        "spring.ai.openai.chat.options.model=${GROQ_MODEL:qwen/qwen3.8-27b}",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class ZelaFixAiLiveIntegrationTest {

    @Autowired
    private ZelaFixAiService aiService;

    @Test
    @EnabledIfEnvironmentVariable(named = "GROQ_API_KEY", matches = "^gsk_.+")
    @DisplayName("Teste Real de Integração Groq Multimodal com Qwen 3.8 27B e imagem rua-esburacada.png")
    void testarGroqMultimodalComImagemReal() throws Exception {
        File imageFile = new File("../docs/rua-esburacada.png");
        if (!imageFile.exists()) {
            imageFile = new File("docs/rua-esburacada.png");
        }
        if (!imageFile.exists()) {
            imageFile = new File("../../docs/rua-esburacada.png");
        }
        if (!imageFile.exists()) {
            imageFile = new File(System.getProperty("user.dir"), "docs/rua-esburacada.png");
        }
        if (!imageFile.exists()) {
            imageFile = new File(System.getProperty("user.dir"), "../docs/rua-esburacada.png");
        }
        System.out.println("Arquivo de imagem carregado: " + imageFile.getAbsolutePath());
        byte[] fotoBytes = Files.readAllBytes(imageFile.toPath());
        MimeType detectedMimeType = ImageMimeTypeDetector.detectar(fotoBytes);
        System.out.println("MimeType detectado via Magic Bytes: " + detectedMimeType + " (" + fotoBytes.length + " bytes)");
        assertEquals(MimeTypeUtils.IMAGE_PNG, detectedMimeType, "Deve detectar image/png para rua-esburacada.png");

        TriagemIaResult resultado = aiService.analisarChamado(
                "PAVIMENTACAO",
                "Crateras profundas na via pública acumulando água",
                "A rua está intransitável cheia de buracos e pedregulhos soltos, com risco de quebrar veículos e causar acidentes graves.",
                "Centro",
                fotoBytes
        );

        System.out.println("==================================================");
        System.out.println("RESULTADO DA TRIAGEM MULTIMODAL GROQ (QWEN 3.8 27B):");
        System.out.println("Prioridade Classificada: " + resultado.prioridade());
        System.out.println("Órgão Governamental: " + resultado.orgaoResponsavel());
        System.out.println("Nome Oficial do Órgão: " + (resultado.orgaoResponsavel() != null ? resultado.orgaoResponsavel().getNomeOficial() : "N/A"));
        System.out.println("Parecer Técnico da IA: " + resultado.parecerIa());
        System.out.println("==================================================");

        assertNotNull(resultado, "O resultado da triagem IA não pode ser nulo");
        assertNotNull(resultado.prioridade(), "A prioridade não pode ser nula");
        assertNotNull(resultado.orgaoResponsavel(), "O órgão responsável não pode ser nulo");
        assertNotNull(resultado.parecerIa(), "O parecer técnico não pode ser nulo");
    }
}
