package fiap.com.br.zelafix.chamado;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:oracle:thin:@localhost:1521/xe",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
@AutoConfigureMockMvc
class ChamadoControllerTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ChamadoService chamadoService;

    private byte[] carregarImagemRuaEsburacada() throws Exception {
        File imageFile = new File("../docs/rua-esburacada.png");
        if (!imageFile.exists()) {
            imageFile = new File("../../docs/rua-esburacada.png");
        }
        if (!imageFile.exists()) {
            imageFile = new File("docs/rua-esburacada.png");
        }
        return Files.readAllBytes(imageFile.toPath());
    }

    private ChamadoResponse criarMockResponse(Long id, String titulo, String descricao) {
        EnderecoResponse enderecoResp = new EnderecoResponse(
                1L, "01001-000", "100", "Rua Principal", "Próximo à praça",
                "Centro", "São Paulo", "SP"
        );

        return new ChamadoResponse(
                id,
                titulo,
                descricao,
                LocalDateTime.now(),
                1L,
                "PAVIMENTACAO",
                1L,
                "Cidadão ZelaFix",
                enderecoResp,
                "/chamados/" + id + "/foto",
                "PENDENTE",
                1,
                true,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("GET /chamados/{id}/foto - Deve retornar a imagem com Content-Type dinâmico image/png e inline")
    void deveRetornarFotoComMimeTypeDinamicoPng() throws Exception {
        byte[] fotoBytes = carregarImagemRuaEsburacada();
        when(chamadoService.getPhoto(1L)).thenReturn(fotoBytes);

        mockMvc.perform(get("/chamados/1/foto"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Content-Disposition", "inline"))
                .andExpect(content().bytes(fotoBytes));
    }

    @Test
    @DisplayName("POST /chamados (multipart) - Deve receber imagem da ocorrência e dados JSON criando o chamado com status 201")
    void deveCriarChamadoViaMultipartComFoto() throws Exception {
        byte[] fotoBytes = carregarImagemRuaEsburacada();
        EnderecoRequest enderecoReq = new EnderecoRequest("01001-000", "100", "Em frente ao mercado", "Rua das Flores");
        ChamadoRequest chamadoReq = new ChamadoRequest(
                "Crateras na via pública",
                "Rua esburacada com risco de acidentes",
                1L,
                enderecoReq,
                null
        );

        MockMultipartFile fotoPart = new MockMultipartFile(
                "foto",
                "rua-esburacada.png",
                MediaType.IMAGE_PNG_VALUE,
                fotoBytes
        );

        MockMultipartFile dadosPart = new MockMultipartFile(
                "dados",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(chamadoReq)
        );

        ChamadoResponse responseMock = criarMockResponse(10L, chamadoReq.titulo(), chamadoReq.descricao());
        when(chamadoService.create(any(ChamadoRequest.class), any(byte[].class), anyString())).thenReturn(responseMock);

        mockMvc.perform(multipart("/chamados")
                        .file(fotoPart)
                        .file(dadosPart)
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idChamado").value(10L))
                .andExpect(jsonPath("$.titulo").value("Crateras na via pública"))
                .andExpect(jsonPath("$.categoriaNome").value("PAVIMENTACAO"))
                .andExpect(jsonPath("$.foto").value("/chamados/10/foto"));
    }

    @Test
    @DisplayName("POST /chamados (json base64) - Deve receber chamado com foto em Base64 e retornar 201 Created")
    void deveCriarChamadoViaJsonComFotoBase64() throws Exception {
        byte[] fotoBytes = carregarImagemRuaEsburacada();
        String fotoBase64 = Base64.getEncoder().encodeToString(fotoBytes);

        EnderecoRequest enderecoReq = new EnderecoRequest("01001-000", "100", "Em frente ao mercado", "Rua das Flores");
        ChamadoRequest chamadoReq = new ChamadoRequest(
                "Crateras na via pública",
                "Rua esburacada com risco de acidentes",
                1L,
                enderecoReq,
                fotoBase64
        );

        ChamadoResponse responseMock = criarMockResponse(11L, chamadoReq.titulo(), chamadoReq.descricao());
        when(chamadoService.create(any(ChamadoRequest.class), anyString())).thenReturn(responseMock);

        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chamadoReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idChamado").value(11L))
                .andExpect(jsonPath("$.titulo").value("Crateras na via pública"))
                .andExpect(jsonPath("$.categoriaNome").value("PAVIMENTACAO"))
                .andExpect(jsonPath("$.foto").value("/chamados/11/foto"));
    }
}
