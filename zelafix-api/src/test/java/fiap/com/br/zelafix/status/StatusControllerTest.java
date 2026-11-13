package fiap.com.br.zelafix.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.status.dto.StatusRequest;
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
class StatusControllerTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatusService statusService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("GET /status - Deve listar todos os status publicamente")
    void deveListarTodosOsStatus() throws Exception {
        when(statusService.findAll()).thenReturn(List.of(new Status(1L, "PENDENTE"), new Status(2L, "CONCLUIDO")));

        mockMvc.perform(get("/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nome").value("PENDENTE"))
                .andExpect(jsonPath("$[1].nome").value("CONCLUIDO"));
    }

    @Test
    @DisplayName("GET /status/{id} - Deve retornar status por ID")
    void deveRetornarStatusPorId() throws Exception {
        when(statusService.findById(1L)).thenReturn(new Status(1L, "PENDENTE"));

        mockMvc.perform(get("/status/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("PENDENTE"));
    }

    @Test
    @DisplayName("GET /status/{id} - Deve retornar 404 quando status não existir")
    void deveRetornar404QuandoStatusNaoExistir() throws Exception {
        when(statusService.findById(99L)).thenThrow(new ResourceNotFoundException("Status não encontrado com id: 99"));

        mockMvc.perform(get("/status/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Status não encontrado com id: 99"));
    }

    @Test
    @DisplayName("POST /status - Deve criar status com sucesso quando usuário for ADMIN")
    void deveCriarStatusComAdmin() throws Exception {
        StatusRequest req = new StatusRequest("TRIAGEM");
        Status criado = new Status(3L, "TRIAGEM");
        when(statusService.create(any(StatusRequest.class))).thenReturn(criado);

        mockMvc.perform(post("/status")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.nome").value("TRIAGEM"));
    }

    @Test
    @DisplayName("POST /status - Deve proibir criação para usuário com perfil comum CIDADAO (403)")
    void deveProibirCriacaoParaCidadao() throws Exception {
        StatusRequest req = new StatusRequest("TRIAGEM");

        mockMvc.perform(post("/status")
                        .with(SecurityMockMvcRequestPostProcessors.user("cidadao@zelafix.com.br").roles("CIDADAO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /status/{id} - Deve atualizar status com sucesso para ADMIN")
    void deveAtualizarStatusComAdmin() throws Exception {
        StatusRequest req = new StatusRequest("EM_ANDAMENTO");
        Status atualizado = new Status(1L, "EM_ANDAMENTO");
        when(statusService.update(eq(1L), any(StatusRequest.class))).thenReturn(atualizado);

        mockMvc.perform(put("/status/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("EM_ANDAMENTO"));
    }

    @Test
    @DisplayName("DELETE /status/{id} - Deve remover status com sucesso para ADMIN")
    void deveExcluirStatusComAdmin() throws Exception {
        doNothing().when(statusService).delete(1L);

        mockMvc.perform(delete("/status/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(statusService).delete(1L);
    }
}
