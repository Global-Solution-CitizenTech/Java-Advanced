package fiap.com.br.zelafix.auth;

import fiap.com.br.zelafix.auth.dto.LoginRequest;
import fiap.com.br.zelafix.auth.dto.LoginResponse;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api"})
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Autenticacao de usuarios, emissao de token JWT e gestao de perfil")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;

    @PostMapping({"/login", "/auth/login"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[PÚBLICO] Autenticar usuário e emitir token Bearer JWT")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.senha())
        );

        String token = tokenService.generateToken(auth.getName());
        Usuario usuario = usuarioService.buscarPorEmail(request.email());
        UsuarioResponse userResponse = UsuarioResponse.fromEntity(usuario);

        return new LoginResponse(token, userResponse);
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PÚBLICO] Cadastro de novo cidadão com emissão imediata de sessão")
    public LoginResponse register(@RequestBody @Valid UsuarioRequest request) {
        UsuarioResponse userResponse = usuarioService.cadastrar(request);
        String token = tokenService.generateToken(request.email());
        return new LoginResponse(token, userResponse);
    }

    @GetMapping("/auth/me")
    @Operation(summary = "[PROTEGIDO] Obter dados do perfil do usuário autenticado")
    public UsuarioResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getSubject();
        Usuario usuario = usuarioService.buscarPorEmail(email);
        return UsuarioResponse.fromEntity(usuario);
    }

    @PutMapping("/auth/me")
    @Operation(summary = "[PROTEGIDO] Atualizar perfil do usuário autenticado")
    public UsuarioResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UsuarioRequest request) {
        String email = jwt.getSubject();
        Usuario usuario = usuarioService.buscarPorEmail(email);
        return usuarioService.atualizarPerfil(usuario.getIdUsuario(), request);
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "[PROTEGIDO] Encerrar sessão ativa")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
