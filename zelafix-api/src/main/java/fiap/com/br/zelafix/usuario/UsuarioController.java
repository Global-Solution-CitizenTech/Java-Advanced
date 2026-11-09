package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.auth.TokenService;
import fiap.com.br.zelafix.auth.dto.LoginResponse;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Endpoints de gerenciamento e perfil de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PÚBLICO] Cadastro de novo cidadão")
    public LoginResponse cadastrar(@RequestBody @Valid UsuarioRequest request) {
        UsuarioResponse userResponse = usuarioService.cadastrar(request);
        String token = tokenService.generateToken(request.email());
        return new LoginResponse(token, userResponse);
    }

    @GetMapping("/me")
    @Operation(summary = "[PROTEGIDO] Retorna perfil do usuário logado")
    public UsuarioResponse meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        Usuario usuario = usuarioService.buscarPorEmail(jwt.getSubject());
        return UsuarioResponse.fromEntity(usuario);
    }

    @PutMapping("/me")
    @Operation(summary = "[PROTEGIDO] Atualiza dados do perfil do usuário logado")
    public UsuarioResponse atualizarPerfil(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid UsuarioRequest request) {
        Usuario usuario = usuarioService.buscarPorEmail(jwt.getSubject());
        return usuarioService.atualizarPerfil(usuario.getIdUsuario(), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PROTEGIDO] Busca detalhes de usuário por id")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        return UsuarioResponse.fromEntity(usuario);
    }

    @GetMapping
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "[GESTOR] Lista paginada de todos os usuários")
    public Page<UsuarioResponse> listarTodos(@PageableDefault(size = 20) Pageable pageable) {
        return usuarioService.listarTodos(pageable);
    }
}
