package fiap.com.br.zelafix.endereco;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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
class EnderecoControllerTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnderecoService enderecoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Endereco endereco;

    @BeforeEach
    void setUp() {
        Estado estado = Estado.builder().id(1L).nome("São Paulo").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Bela Vista").cidade(cidade).build();

        endereco = Endereco.builder()
                .id(1L)
                .cep("01310100")
                .numero("1000")
                .rua("Avenida Paulista")
                .referencia("Masp")
                .bairro(bairro)
                .build();
    }

    @Test
    @DisplayName("GET /enderecos - Deve retornar 200 OK com paginação para ADMIN")
    void deveRetornarEnderecosParaAdmin() throws Exception {
        when(enderecoService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(endereco)));

        mockMvc.perform(get("/enderecos")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].cep").value("01310100"))
                .andExpect(jsonPath("$.content[0].rua").value("Avenida Paulista"));
    }

    @Test
    @DisplayName("GET /enderecos - Deve retornar 403 Forbidden para cidadão comum")
    void deveBloquearListagemDeEnderecosParaCidadao() throws Exception {
        mockMvc.perform(get("/enderecos")
                        .with(SecurityMockMvcRequestPostProcessors.user("cidadao@zelafix.com.br").roles("CIDADAO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /enderecos/{id} - Deve retornar endereço por ID para ADMIN")
    void deveRetornarEnderecoPorIdParaAdmin() throws Exception {
        when(enderecoService.findById(1L)).thenReturn(endereco);

        mockMvc.perform(get("/enderecos/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.rua").value("Avenida Paulista"));
    }

    @Test
    @DisplayName("GET /enderecos/{id} - Deve retornar 404 quando endereço não existir")
    void deveRetornar404QuandoEnderecoNaoExistir() throws Exception {
        when(enderecoService.findById(99L)).thenThrow(new ResourceNotFoundException("Endereço com id 99 não encontrado."));

        mockMvc.perform(get("/enderecos/99")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Endereço com id 99 não encontrado."));
    }

    @Test
    @DisplayName("POST /enderecos - Deve criar endereço com sucesso para ADMIN")
    void deveCriarEnderecoComAdmin() throws Exception {
        EnderecoRequest req = new EnderecoRequest("01310-100", "1000", "Avenida Paulista", "Masp");
        when(enderecoService.create(any(EnderecoRequest.class))).thenReturn(endereco);

        mockMvc.perform(post("/enderecos")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("PUT /enderecos/{id} - Deve atualizar endereço com sucesso para ADMIN")
    void deveAtualizarEnderecoComAdmin() throws Exception {
        EnderecoRequest req = new EnderecoRequest("01310-100", "1002", "Avenida Paulista", "Em frente ao Masp");
        when(enderecoService.update(eq(1L), any(EnderecoRequest.class))).thenReturn(endereco);

        mockMvc.perform(put("/enderecos/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("DELETE /enderecos/{id} - Deve remover endereço para ADMIN")
    void deveRemoverEnderecoComAdmin() throws Exception {
        doNothing().when(enderecoService).delete(1L);

        mockMvc.perform(delete("/enderecos/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(enderecoService).delete(1L);
    }
}
