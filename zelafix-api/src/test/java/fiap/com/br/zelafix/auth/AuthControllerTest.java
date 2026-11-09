package fiap.com.br.zelafix.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import fiap.com.br.zelafix.endereco.Endereco;
import fiap.com.br.zelafix.endereco.bairro.Bairro;
import fiap.com.br.zelafix.endereco.cidade.Cidade;
import fiap.com.br.zelafix.endereco.estado.Estado;
import fiap.com.br.zelafix.auth.dto.LoginRequest;
import fiap.com.br.zelafix.telefone.Telefone;
import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve autenticar com sucesso e retornar token Bearer JWT e dados do usuário")
    void deveAutenticarComSucesso() throws Exception {
        LoginRequest request = new LoginRequest("cidadao@zelafix.com.br", "Senha@123");

        Estado estado = Estado.builder().id(1L).nome("SP").build();
        Cidade cidade = Cidade.builder().id(1L).nome("São Paulo").estado(estado).build();
        Bairro bairro = Bairro.builder().id(1L).nome("Pinheiros").cidade(cidade).build();
        Endereco endereco = Endereco.builder()
                .id(1L)
                .rua("Rua dos Pinheiros")
                .numero("720")
                .referencia("Em frente à praça")
                .cep("05422001")
                .bairro(bairro)
                .build();

        Telefone mockTelefone = Telefone.builder().numDdd("11").numTel("987654321").build();
        Usuario mockUsuario = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Carlos Munícipe")
                .email("cidadao@zelafix.com.br")
                .cargo(CargoUsuario.CIDADAO)
                .endereco(endereco)
                .telefones(java.util.List.of(mockTelefone))
                .build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("cidadao@zelafix.com.br", "Senha@123"));
        when(tokenService.generateToken("cidadao@zelafix.com.br")).thenReturn("mock-jwt-token-xyz");
        when(usuarioService.findByEmail("cidadao@zelafix.com.br")).thenReturn(mockUsuario);

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-xyz"))
                .andExpect(jsonPath("$.user.nome").value("Carlos Munícipe"))
                .andExpect(jsonPath("$.user.email").value("cidadao@zelafix.com.br"))
                .andExpect(jsonPath("$.user.cargo").value("CIDADAO"));
    }

    @Test
    @DisplayName("Deve retornar 401 Unauthorized para credenciais incorretas")
    void deveRetornar401ParaCredenciaisInvalidas() throws Exception {
        LoginRequest request = new LoginRequest("cidadao@zelafix.com.br", "SenhaIncorreta");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }
}
