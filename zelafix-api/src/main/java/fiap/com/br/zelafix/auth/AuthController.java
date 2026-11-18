package fiap.com.br.zelafix.auth;

import fiap.com.br.zelafix.config.WorkerWakeUpService;
import fiap.com.br.zelafix.usuario.Usuario;
import fiap.com.br.zelafix.usuario.UsuarioService;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação de usuários e geração de token JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;
    private final WorkerWakeUpService workerWakeUpService;

    public record LoginRequest(
            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "Formato de e-mail inválido.")
            String email,

            @NotBlank(message = "A senha é obrigatória.")
            String senha
    ) {}

    public record LoginResponse(String token, UsuarioResponse user) {}

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[PÚBLICO] Autenticar usuário e retornar token Bearer JWT com dados do perfil")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.senha())
        );

        String token = tokenService.generateToken(auth.getName());
        Usuario usuario = usuarioService.findByEmail(auth.getName());
        workerWakeUpService.wakeUpAsync();

        return new LoginResponse(token, UsuarioResponse.fromEntity(usuario));
    }
}
