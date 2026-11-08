package fiap.com.br.zelafix.upload;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve realizar upload de foto com sucesso e permitir sua visualização via GET")
    void deveFazerUploadDeFotoComSucesso() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "buraco_rua.jpg",
                "image/jpeg",
                "conteudo-ficticio-da-foto".getBytes()
        );

        String jsonResponse = mockMvc.perform(multipart("/uploads")
                        .file(arquivo)
                        .with(jwt().jwt(builder -> builder.subject("cidadao@zelafix.com.br"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(jsonPath("$.nomeArquivo").exists())
                .andExpect(jsonPath("$.tipoConteudo").value("image/jpeg"))
                .andReturn().getResponse().getContentAsString();

        String nomeArquivo = JsonPath.read(jsonResponse, "$.nomeArquivo");

        mockMvc.perform(get("/uploads/" + nomeArquivo))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }
}
