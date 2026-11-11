package fiap.com.br.zelafix.auth;

import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .nmUsuario("Carlos Cidadão")
                .email("cidadao@zelafix.com.br")
                .senha("$2a$10$encodedpassword")
                .cargo(CargoUsuario.CIDADAO)
                .build();
    }

    @Test
    @DisplayName("Deve carregar usuário por username/email com sucesso e converter para UserDetails com autoridade ROLE_")
    void deveCarregarUsuarioPorUsernameComSucesso() {
        when(usuarioRepository.findByEmail("cidadao@zelafix.com.br")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = authService.loadUserByUsername("cidadao@zelafix.com.br");

        assertNotNull(userDetails);
        assertEquals("cidadao@zelafix.com.br", userDetails.getUsername());
        assertEquals("$2a$10$encodedpassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_CIDADAO")));

        verify(usuarioRepository).findByEmail("cidadao@zelafix.com.br");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não for encontrado por email")
    void deveLancarUsernameNotFoundExceptionQuandoUsuarioNaoEncontrado() {
        when(usuarioRepository.findByEmail("inexistente@zelafix.com.br")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> authService.loadUserByUsername("inexistente@zelafix.com.br")
        );

        assertTrue(exception.getMessage().contains("inexistente@zelafix.com.br"));
        verify(usuarioRepository).findByEmail("inexistente@zelafix.com.br");
    }
}
