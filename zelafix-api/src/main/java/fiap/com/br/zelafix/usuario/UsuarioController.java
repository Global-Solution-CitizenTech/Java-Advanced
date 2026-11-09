package fiap.com.br.zelafix.usuario;

import fiap.com.br.zelafix.auth.TokenService;
import fiap.com.br.zelafix.usuario.dto.CargoUpdateRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioRequest;
import fiap.com.br.zelafix.usuario.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Gestão de usuários, perfil pessoal e administração do sistema")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final TokenService tokenService;

    // =========================================================================
    // 1. FLUXO DO USUÁRIO LOGADO (/me)
    // =========================================================================

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[AUTENTICADO] Buscar perfil do usuário autenticado")
    public UsuarioResponse getMe(Authentication authentication) {
        return UsuarioResponse.fromEntity(usuarioService.findByEmail(authentication.getName()));
    }

    @PutMapping("/me")
    @Operation(summary = "[AUTENTICADO] Atualizar perfil do usuário autenticado")
    public ResponseEntity<UsuarioResponse> updateMe(Authentication authentication, @Valid @RequestBody UsuarioRequest request) {
        Usuario atualizado = usuarioService.update(authentication.getName(), request);
        return ResponseEntity.ok()
                .header("X-Novo-Token", tokenService.generateToken(atualizado.getEmail()))
                .body(UsuarioResponse.fromEntity(atualizado));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[AUTENTICADO] Deletar perfil do usuário autenticado")
    public void deleteMe(Authentication authentication) {
        usuarioService.delete(authentication.getName());
    }

    // =========================================================================
    // 2. CADASTRO PÚBLICO
    // =========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PÚBLICO] Criar novo usuário (cadastro de cidadão)")
    public UsuarioResponse create(@Valid @RequestBody UsuarioRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.create(request));
    }

    // =========================================================================
    // 3. OPERAÇÕES EXCLUSIVAS DE ADMINISTRAÇÃO (ROLE ADMIN)
    // =========================================================================

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar usuário por ID")
    public UsuarioResponse findById(@PathVariable Long id) {
        return UsuarioResponse.fromEntity(usuarioService.findById(id));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Atualizar usuário por ID")
    public UsuarioResponse update(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Deletar usuário por ID")
    public void delete(@PathVariable Long id) {
        usuarioService.delete(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Listar todos os usuários com paginação e ordenação")
    public Page<UsuarioResponse> findAll(
            @PageableDefault(size = 10, page = 0, sort = "nmUsuario", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return usuarioService.findAll(pageable).map(UsuarioResponse::fromEntity);
    }

    @GetMapping("/by-email")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar usuário por e-mail")
    public UsuarioResponse findByEmail(@RequestParam String email) {
        return UsuarioResponse.fromEntity(usuarioService.findByEmail(email));
    }

    @PatchMapping("/{id}/cargo")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Alterar cargo/perfil do usuário")
    public UsuarioResponse updateCargo(@PathVariable Long id, @Valid @RequestBody CargoUpdateRequest request) {
        return UsuarioResponse.fromEntity(usuarioService.updateCargo(id, request.cargo()));
    }
}
