package fiap.com.br.zelafix.usuario;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.auth.TokenService;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.usuario.dto.CargoUpdateRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class UsuarioControllerTest {

    @MockitoBean
    private javax.sql.DataSource dataSource;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private TokenService tokenService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Usuario usuario;
    private UsuarioRequest request;

    @BeforeEach
    void setUp() {
        Estado estado = Estado.builder().id(1L).nome("São Paulo").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Sé").cidade(cidade).build();
        Endereco endereco = Endereco.builder()
                .id(1L)
                .cep("01001000")
                .numero("100")
                .rua("Praça da Sé")
                .referencia("Centro")
                .bairro(bairro)
                .build();

        Telefone telefone = Telefone.builder().numDdd("11").numTel("987654321").build();

        usuario = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Enzo Munícipe")
                .email("enzo@zelafix.com.br")
                .senha("hash")
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endereco)
                .telefone(telefone)
                .build();

        EnderecoRequest endReq = new EnderecoRequest("01001-000", "100", "Praça da Sé", "Centro");
        request = new UsuarioRequest("Enzo Munícipe", "enzo@zelafix.com.br", "Senha@123", CargoUsuario.CIDADAO, "11", "987654321", endReq);
    }

    @Test
    @DisplayName("GET /usuarios/me - Deve retornar perfil do usuário logado")
    void deveRetornarPerfilDoUsuarioLogado() throws Exception {
        when(usuarioService.findByEmail("enzo@zelafix.com.br")).thenReturn(usuario);

        mockMvc.perform(get("/usuarios/me")
                        .with(SecurityMockMvcRequestPostProcessors.user("enzo@zelafix.com.br").roles("CIDADAO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nome").value("Enzo Munícipe"))
                .andExpect(jsonPath("$.email").value("enzo@zelafix.com.br"));
    }

    @Test
    @DisplayName("PUT /usuarios/me - Deve atualizar perfil do usuário logado e retornar header X-Novo-Token")
    void deveAtualizarPerfilDoUsuarioLogado() throws Exception {
        when(usuarioService.update(eq("enzo@zelafix.com.br"), any(UsuarioRequest.class))).thenReturn(usuario);
        when(tokenService.generateToken("enzo@zelafix.com.br")).thenReturn("jwt.token.novo");

        mockMvc.perform(put("/usuarios/me")
                        .with(SecurityMockMvcRequestPostProcessors.user("enzo@zelafix.com.br").roles("CIDADAO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Novo-Token", "jwt.token.novo"))
                .andExpect(jsonPath("$.nome").value("Enzo Munícipe"));
    }

    @Test
    @DisplayName("DELETE /usuarios/me - Deve excluir conta do usuário logado")
    void deveExcluirContaDoUsuarioLogado() throws Exception {
        doNothing().when(usuarioService).delete("enzo@zelafix.com.br");

        mockMvc.perform(delete("/usuarios/me")
                        .with(SecurityMockMvcRequestPostProcessors.user("enzo@zelafix.com.br").roles("CIDADAO")))
                .andExpect(status().isNoContent());

        verify(usuarioService).delete("enzo@zelafix.com.br");
    }

    @Test
    @DisplayName("POST /usuarios - Deve permitir cadastro público com 201 Created")
    void devePermitirCadastroPublico() throws Exception {
        when(usuarioService.create(any(UsuarioRequest.class))).thenReturn(usuario);

        mockMvc.perform(post("/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("enzo@zelafix.com.br"));
    }

    @Test
    @DisplayName("GET /usuarios/{id} - Deve retornar usuário por ID para ADMIN")
    void deveRetornarUsuarioPorIdParaAdmin() throws Exception {
        when(usuarioService.findById(1L)).thenReturn(usuario);

        mockMvc.perform(get("/usuarios/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @DisplayName("GET /usuarios/{id} - Deve retornar 403 Forbidden para CIDADAO")
    void deveBloquearAcessoPorIdParaCidadao() throws Exception {
        mockMvc.perform(get("/usuarios/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("enzo@zelafix.com.br").roles("CIDADAO")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /usuarios - Deve listar todos os usuários com paginação para ADMIN")
    void deveListarUsuariosPaginadosParaAdmin() throws Exception {
        when(usuarioService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(usuario)));

        mockMvc.perform(get("/usuarios")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("enzo@zelafix.com.br"));
    }

    @Test
    @DisplayName("GET /usuarios/by-email - Deve buscar usuário por e-mail para ADMIN")
    void deveBuscarUsuarioPorEmailParaAdmin() throws Exception {
        when(usuarioService.findByEmail("enzo@zelafix.com.br")).thenReturn(usuario);

        mockMvc.perform(get("/usuarios/by-email")
                        .param("email", "enzo@zelafix.com.br")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("enzo@zelafix.com.br"));
    }

    @Test
    @DisplayName("PATCH /usuarios/{id}/cargo - Deve atualizar cargo do usuário para ADMIN")
    void deveAtualizarCargoParaAdmin() throws Exception {
        CargoUpdateRequest cargoReq = new CargoUpdateRequest("GESTOR");
        Usuario gestor = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Enzo Munícipe")
                .email("enzo@zelafix.com.br")
                .cargo(CargoUsuario.GESTOR)
                .endereco(usuario.getEndereco())
                .telefone(usuario.getTelefone())
                .build();

        when(usuarioService.updateCargo(1L, "GESTOR")).thenReturn(gestor);

        mockMvc.perform(patch("/usuarios/1/cargo")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cargoReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("GESTOR"));
    }

    @Test
    @DisplayName("DELETE /usuarios/{id} - Deve excluir usuário por ID para ADMIN")
    void deveExcluirUsuarioPorIdParaAdmin() throws Exception {
        doNothing().when(usuarioService).delete(1L);

        mockMvc.perform(delete("/usuarios/1")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin@zelafix.com.br").roles("ADMIN")))
                .andExpect(status().isNoContent());

        verify(usuarioService).delete(1L);
    }
}
