package fiap.com.br.zelafix.categoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class CategoriaControllerTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /categorias - Deve retornar 200 OK com lista de categorias publicamente")
    void deveRetornarTodasAsCategorias() throws Exception {
        Categoria cat = new Categoria(1L, "Asfalto e Pavimentação");
        when(categoriaService.findAll()).thenReturn(List.of(cat));

        mockMvc.perform(get("/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("Asfalto e Pavimentação"));
    }

    @Test
    @DisplayName("GET /categorias/{id} - Deve retornar 200 OK quando categoria existe")
    void deveRetornarCategoriaPorId() throws Exception {
        Categoria cat = new Categoria(1L, "Iluminação");
        when(categoriaService.findById(1L)).thenReturn(cat);

        mockMvc.perform(get("/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Iluminação"));
    }

    @Test
    @DisplayName("GET /categorias/{id} - Deve retornar 404 Not Found quando categoria não existe")
    void deveRetornar404QuandoCategoriaNaoExiste() throws Exception {
        when(categoriaService.findById(99L)).thenThrow(new ResourceNotFoundException("Categoria não encontrada com id: 99"));

        mockMvc.perform(get("/categorias/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Categoria não encontrada com id: 99"));
    }

    @Test
    @DisplayName("POST /categorias - Deve retornar 201 Created quando usuário for ADMIN")
    void deveCriarCategoriaComAdmin() throws Exception {
        CategoriaRequest req = new CategoriaRequest("Saneamento");
        Categoria salva = new Categoria(5L, "Saneamento");
        when(categoriaService.create(any(CategoriaRequest.class))).thenReturn(salva);

        mockMvc.perform(post("/categorias")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.nome").value("Saneamento"));
    }

    @Test
    @DisplayName("POST /categorias - Deve retornar 403 Forbidden para cidadão sem permissão ADMIN")
    void deveBloquearCriacaoDeCategoriaParaCidadao() throws Exception {
        CategoriaRequest req = new CategoriaRequest("Saneamento");

        mockMvc.perform(post("/categorias")
                        .with(SecurityMockMvcRequestPostProcessors.user("cidadao@zelafix.com.br").roles("CIDADAO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /categorias - Deve retornar 400 Bad Request se nome for em branco")
    void deveRejeitarCategoriaComNomeInvalido() throws Exception {
        CategoriaRequest req = new CategoriaRequest("");

        mockMvc.perform(post("/categorias")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros").isArray());
    }

    @Test
    @DisplayName("PUT /categorias/{id} - Deve retornar 200 OK com ADMIN")
    void deveAtualizarCategoriaComAdmin() throws Exception {
        CategoriaRequest req = new CategoriaRequest("Árvores e Podas");
        Categoria atualizada = new Categoria(1L, "Árvores e Podas");
        when(categoriaService.update(eq(1L), any(CategoriaRequest.class))).thenReturn(atualizada);

        mockMvc.perform(put("/categorias/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Árvores e Podas"));
    }

    @Test
    @DisplayName("DELETE /categorias/{id} - Deve retornar 204 No Content com ADMIN")
    void deveExcluirCategoriaComAdmin() throws Exception {
        doNothing().when(categoriaService).delete(1L);

        mockMvc.perform(delete("/categorias/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(categoriaService).delete(1L);
    }
}
