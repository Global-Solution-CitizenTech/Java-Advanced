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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Autenticacao e emissao de tokens JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;

    @PostMapping({"/login", "/auth/login"})
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Autenticar usuário e obter token JWT")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.senha())
        );

        String token = tokenService.generateToken(auth.getName());
        Usuario usuario = usuarioService.buscarPorEmail(request.email());
        return new LoginResponse(token, new UsuarioResponse(usuario));
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registro de novo usuário")
    public LoginResponse register(@RequestBody @Valid UsuarioRequest request) {
        UsuarioResponse userResponse = usuarioService.cadastrar(request);
        String token = tokenService.generateToken(request.email());
        return new LoginResponse(token, userResponse);
    }
}
