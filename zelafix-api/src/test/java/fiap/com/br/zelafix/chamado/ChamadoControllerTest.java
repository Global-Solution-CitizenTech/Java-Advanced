package fiap.com.br.zelafix.chamado;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.chamado.dto.ChamadoFiltroRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoStatusRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.access.AccessDeniedException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
        return criarMockResponse(id, titulo, descricao, "PENDENTE");
    }

    private ChamadoResponse criarMockResponse(Long id, String titulo, String descricao, String status) {
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
                "ALTA",
                1L,
                "Secretaria de Obras",
                1L,
                "Cidadão ZelaFix",
                enderecoResp,
                "/chamados/" + id + "/foto",
                status,
                1,
                true,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("GET /chamados - Deve listar chamados paginados com filtros dinâmicos")
    void deveListarChamadosPaginadosComFiltros() throws Exception {
        ChamadoResponse resp = criarMockResponse(1L, "Buraco na rua", "Cratera profunda na via");
        Page<ChamadoResponse> page = new PageImpl<>(List.of(resp), PageRequest.of(0, 10), 1);

        when(chamadoService.findAll(
                eq(new ChamadoFiltroRequest("PENDENTE", "PAVIMENTACAO", "buraco", true, null, null, null)),
                eq("cidadao@zelafix.com"),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/chamados")
                        .param("status", "PENDENTE")
                        .param("categoria", "PAVIMENTACAO")
                        .param("busca", "buraco")
                        .param("apenasMeus", "true")
                        .param("page", "0")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].idChamado").value(1L))
                .andExpect(jsonPath("$.content[0].titulo").value("Buraco na rua"))
                .andExpect(jsonPath("$.content[0].categoriaNome").value("PAVIMENTACAO"));
    }

    @Test
    @DisplayName("GET /chamados/{id} - Deve retornar 200 OK com dados detalhados quando chamado existir")
    void deveBuscarChamadoPorIdComSucesso() throws Exception {
        ChamadoResponse resp = criarMockResponse(1L, "Vazamento de água", "Cano rompido na calçada");
        when(chamadoService.findDetailsById(1L, "cidadao@zelafix.com")).thenReturn(resp);

        mockMvc.perform(get("/chamados/1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idChamado").value(1L))
                .andExpect(jsonPath("$.titulo").value("Vazamento de água"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    @DisplayName("GET /chamados/{id} - Deve retornar 404 Not Found quando chamado não existir")
    void deveRetornar404QuandoChamadoNaoExistir() throws Exception {
        when(chamadoService.findDetailsById(999L, "cidadao@zelafix.com"))
                .thenThrow(new ResourceNotFoundException("Chamado com id 999 não encontrado."));

        mockMvc.perform(get("/chamados/999")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Chamado com id 999 não encontrado."));
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
    @DisplayName("GET /chamados/{id}/foto - Deve retornar 404 Not Found quando chamado ou foto não existirem")
    void deveRetornar404QuandoFotoNaoExistir() throws Exception {
        when(chamadoService.getPhoto(999L))
                .thenThrow(new ResourceNotFoundException("Chamado com id 999 não encontrado."));

        mockMvc.perform(get("/chamados/999/foto"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Chamado com id 999 não encontrado."));
    }

    @Test
    @DisplayName("POST /chamados (multipart) - Deve aguardar triagem IA via DeferredResult e retornar 201 Created com ChamadoResponse")
    void deveCriarChamadoViaMultipartComFoto() throws Exception {
        byte[] fotoBytes = carregarImagemRuaEsburacada();

        MockMultipartFile fotoPart = new MockMultipartFile(
                "foto",
                "rua-esburacada.png",
                MediaType.IMAGE_PNG_VALUE,
                fotoBytes
        );

        ChamadoResponse resp = criarMockResponse(1L, "Cratera na via", "Buraco profundo no asfalto", "EM_ANALISE");
        DeferredResult<ChamadoResponse> deferred = new DeferredResult<>();
        deferred.setResult(resp);

        when(chamadoService.create(any(EnderecoRequest.class), any(byte[].class), eq("cidadao@zelafix.com")))
                .thenReturn(deferred);

        MvcResult mvcResult = mockMvc.perform(multipart("/chamados")
                        .file(fotoPart)
                        .param("cep", "01001-000")
                        .param("numero", "100")
                        .param("referencia", "Em frente ao mercado")
                        .param("rua", "Rua das Flores")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idChamado").value(1L))
                .andExpect(jsonPath("$.titulo").value("Cratera na via"))
                .andExpect(jsonPath("$.status").value("EM_ANALISE"));
    }

    @Test
    @DisplayName("POST /chamados (multipart) - Deve retornar 400 Bad Request em JSON quando a IA recusar a imagem")
    void deveRetornar400QuandoIaRecusarNaCriacao() throws Exception {
        MockMultipartFile fotoPart = new MockMultipartFile(
                "foto",
                "rua-esburacada.png",
                MediaType.IMAGE_PNG_VALUE,
                carregarImagemRuaEsburacada()
        );

        DeferredResult<ChamadoResponse> deferred = new DeferredResult<>();
        deferred.setErrorResult(new IllegalArgumentException("A foto enviada é uma captura de tela de jogo."));

        when(chamadoService.create(any(EnderecoRequest.class), any(byte[].class), eq("cidadao@zelafix.com")))
                .thenReturn(deferred);

        MvcResult mvcResult = mockMvc.perform(multipart("/chamados")
                        .file(fotoPart)
                        .param("cep", "01001-000")
                        .param("numero", "100")
                        .param("referencia", "Em frente ao mercado")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("A foto enviada é uma captura de tela de jogo."));
    }

    @Test
    @DisplayName("POST /chamados (multipart) - Deve retornar 400 Bad Request quando campos do endereço forem inválidos")
    void deveValidarCamposEnderecoNoCreateMultipart() throws Exception {
        MockMultipartFile fotoPart = new MockMultipartFile(
                "foto",
                "rua-esburacada.png",
                MediaType.IMAGE_PNG_VALUE,
                carregarImagemRuaEsburacada()
        );

        mockMvc.perform(multipart("/chamados")
                        .file(fotoPart)
                        .param("cep", "invalido")
                        .param("numero", "")
                        .param("referencia", "")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @DisplayName("POST /chamados (multipart) - Deve retornar 400 Bad Request quando part 'foto' não for enviada")
    void deveRetornar400QuandoPartFotoEstiverAusenteNoCreate() throws Exception {
        mockMvc.perform(multipart("/chamados")
                        .param("cep", "01001-000")
                        .param("numero", "100")
                        .param("referencia", "Em frente ao mercado")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("O arquivo ou campo obrigatório 'foto' não foi enviado."));
    }

    @Test
    @DisplayName("PUT /chamados/{id} - Deve atualizar dados do chamado com retorno 200 OK")
    void deveAtualizarChamadoComSucesso() throws Exception {
        EnderecoRequest enderecoReq = new EnderecoRequest("01001-000", "100", "Em frente ao mercado", "Rua das Flores");
        ChamadoRequest chamadoReq = new ChamadoRequest(
                "Buraco concertado parcialmente",
                "Asfalto ainda apresenta desnível",
                "VIAS_E_PAVIMENTACAO",
                enderecoReq
        );

        ChamadoResponse responseMock = criarMockResponse(1L, chamadoReq.titulo(), chamadoReq.descricao());
        when(chamadoService.update(eq(1L), any(ChamadoRequest.class), eq("cidadao@zelafix.com"))).thenReturn(responseMock);

        mockMvc.perform(put("/chamados/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chamadoReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idChamado").value(1L))
                .andExpect(jsonPath("$.titulo").value("Buraco concertado parcialmente"));
    }

    @Test
    @DisplayName("PATCH /chamados/{id}/status - Deve retornar 200 OK quando usuário for GESTOR")
    void deveAtualizarStatusChamadoComGestor() throws Exception {
        ChamadoStatusRequest statusReq = new ChamadoStatusRequest("EM_EXECUCAO", "Equipe técnica enviada ao local.");
        ChamadoResponse resp = criarMockResponse(1L, "Crateras na via pública", "Descrição", "EM_EXECUCAO");

        when(chamadoService.updateStatus(eq(1L), any(ChamadoStatusRequest.class), eq("gestor@zelafix.com")))
                .thenReturn(resp);

        mockMvc.perform(patch("/chamados/1/status")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_GESTOR"))
                                .jwt(jwt -> jwt.subject("gestor@zelafix.com").claim("role", "GESTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idChamado").value(1L))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    @DisplayName("PATCH /chamados/{id}/status - Deve retornar 403 Forbidden em JSON quando usuário for CIDADAO")
    void deveBloquearAtualizacaoStatusParaCidadao() throws Exception {
        ChamadoStatusRequest statusReq = new ChamadoStatusRequest("CONCLUIDO", "Tentativa indevida de conclusão por cidadão.");

        mockMvc.perform(patch("/chamados/1/status")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_CIDADAO"))
                                .jwt(jwt -> jwt.subject("cidadao@zelafix.com").claim("role", "CIDADAO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Você não possui permissão para acessar este recurso."));
    }

    @Test
    @DisplayName("PUT /chamados/{id} - Deve retornar 403 Forbidden quando outro cidadão tentar editar")
    void deveBloquearEdicaoChamadoParaOutroUsuario() throws Exception {
        ChamadoRequest chamadoReq = new ChamadoRequest(
                "Tentativa indevida",
                "Descrição",
                "VIAS_E_PAVIMENTACAO",
                new EnderecoRequest("01001-000", "100", "Em frente ao mercado", "Rua das Flores")
        );

        when(chamadoService.update(eq(1L), any(ChamadoRequest.class), eq("outro@zelafix.com")))
                .thenThrow(new AccessDeniedException("Apenas o autor do chamado ou um gestor/administrador pode editá-lo."));

        mockMvc.perform(put("/chamados/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chamadoReq))
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("outro@zelafix.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Apenas o autor do chamado ou um gestor/administrador pode editá-lo."));
    }

    @Test
    @DisplayName("PATCH /chamados/{id}/status - Deve retornar 400 Bad Request se status for inválido")
    void deveRejeitarStatusInvalido() throws Exception {
        String invalidJson = """
                {
                    "status": "STATUS_INVENTADO",
                    "observacao": "Tentativa de colocar status inexistente"
                }
                """;

        mockMvc.perform(patch("/chamados/1/status")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_GESTOR"))
                                .jwt(jwt -> jwt.subject("gestor@zelafix.com").claim("role", "GESTOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @DisplayName("POST /chamados/{id}/apoiar - Deve apoiar chamado comunitário com retorno 200 OK")
    void deveApoiarChamadoComSucesso() throws Exception {
        ChamadoResponse resp = criarMockResponse(1L, "Poste apagado", "Lâmpada queimada");
        when(chamadoService.apoiar(1L, "cidadao@zelafix.com")).thenReturn(resp);

        mockMvc.perform(post("/chamados/1/apoiar")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idChamado").value(1L))
                .andExpect(jsonPath("$.apoiadoPorMim").value(true));
    }

    @Test
    @DisplayName("DELETE /chamados/{id} - Deve excluir chamado com retorno 204 No Content quando usuário for AUTOR ou ADMIN")
    void deveExcluirChamadoComAdmin() throws Exception {
        doNothing().when(chamadoService).delete(1L, "admin@zelafix.com");

        mockMvc.perform(delete("/chamados/1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(jwt -> jwt.subject("admin@zelafix.com").claim("role", "ADMIN"))))
                .andExpect(status().isNoContent());

        verify(chamadoService).delete(1L, "admin@zelafix.com");
    }

    @Test
    @DisplayName("DELETE /chamados/{id} - Deve retornar 403 Forbidden quando usuário não for o autor nem ADMIN")
    void deveBloquearExclusaoChamadoParaGestor() throws Exception {
        doThrow(new AccessDeniedException("Apenas o autor do chamado ou um administrador pode excluí-lo."))
                .when(chamadoService).delete(1L, "gestor@zelafix.com");

        mockMvc.perform(delete("/chamados/1")
                        .with(SecurityMockMvcRequestPostProcessors.jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_GESTOR"))
                                .jwt(jwt -> jwt.subject("gestor@zelafix.com").claim("role", "GESTOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /chamados/estatisticas - Deve retornar consolidação estatística com 200 OK")
    void deveRetornarEstatisticas() throws Exception {
        EstatisticasResponse stats = EstatisticasResponse.of(15L, 5L, 4L, 5L, 1L, 30L);
        when(chamadoService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/chamados/estatisticas")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject("cidadao@zelafix.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15L))
                .andExpect(jsonPath("$.abertos").value(5L))
                .andExpect(jsonPath("$.emAndamento").value(4L))
                .andExpect(jsonPath("$.resolvidos").value(5L))
                .andExpect(jsonPath("$.rejeitados").value(1L))
                .andExpect(jsonPath("$.totalApoios").value(30L));
    }

    @Test
    @DisplayName("GET /chamados - Deve retornar 401 Unauthorized sem token ou com Bearer inválido")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/chamados"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/chamados")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
