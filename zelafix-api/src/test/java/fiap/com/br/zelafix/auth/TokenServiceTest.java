package fiap.com.br.zelafix.auth;

import fiap.com.br.zelafix.usuario.CargoUsuario;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TokenService tokenService;

    @Test
    @DisplayName("Deve gerar token JWT com as claims corretas (sub, userId, nome, role)")
    void deveGerarTokenJwtComClaimsCorretas() {
        Usuario usuario = Usuario.builder()
                .idUsuario(42L)
                .nmUsuario("Maria Teste")
                .email("maria@zelafix.com.br")
                .cargo(CargoUsuario.ADMIN)
                .build();

        when(usuarioRepository.findByEmail("maria@zelafix.com.br")).thenReturn(Optional.of(usuario));

        Jwt jwtMock = Jwt.withTokenValue("mocked.jwt.token")
                .header("alg", "RS256")
                .claim("sub", "maria@zelafix.com.br")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        String token = tokenService.generateToken("maria@zelafix.com.br");

        assertEquals("mocked.jwt.token", token);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        var claims = captor.getValue().getClaims();
        assertEquals("zelafix-api", claims.getClaim("iss"));
        assertEquals("maria@zelafix.com.br", claims.getSubject());
        assertEquals(Long.valueOf(42L), claims.getClaim("userId"));
        assertEquals("Maria Teste", claims.getClaim("nome"));
        assertEquals("ADMIN", claims.getClaim("role"));
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando usuário não existir ao gerar token")
    void deveLancarExcecaoQuandoUsuarioNaoExistir() {
        when(usuarioRepository.findByEmail("inexistente@zelafix.com.br")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> tokenService.generateToken("inexistente@zelafix.com.br"));
    }
}
