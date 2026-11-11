package fiap.com.br.zelafix.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("Deve instanciar PasswordEncoder BCrypt e validar hash de senha")
    void deveInstanciarPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String hash = encoder.encode("Senha@123");

        assertNotNull(hash);
        assertTrue(encoder.matches("Senha@123", hash));
    }

    @Test
    @DisplayName("Deve instanciar JwtEncoder e JwtDecoder a partir de par de chaves RSA")
    void deveInstanciarJwtEncoderEDecoder() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        SecurityConfig.RsaKeyProperties props = new SecurityConfig.RsaKeyProperties(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate()
        );

        assertNotNull(securityConfig.jwtDecoder(props));
        assertNotNull(securityConfig.jwtEncoder(props));
    }

    @Test
    @DisplayName("Deve converter claim 'role' do JWT em autoridade ROLE_ correspondente")
    void deveConverterRoleDoJwtParaGrantedAuthority() {
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", "gestor@zelafix.com.br", "role", "gestor")
        );

        var auth = converter.convert(jwt);

        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GESTOR".equals(a.getAuthority())));
    }

    @Test
    @DisplayName("Deve obter AuthenticationManager a partir de AuthenticationConfiguration")
    void deveObterAuthenticationManager() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(manager);

        assertEquals(manager, securityConfig.authenticationManager(config));
    }
}
