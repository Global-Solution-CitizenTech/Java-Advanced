package fiap.com.br.zelafix.comentario;

import fiap.com.br.zelafix.comentario.dto.ComentarioRequest;
import fiap.com.br.zelafix.comentario.dto.ComentarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/chamados/{chamadoId}/comentarios", "/api/chamados/{chamadoId}/comentarios"})
@RequiredArgsConstructor
@Tag(name = "Comentarios", description = "Comentários da comunidade e munícipes nas ocorrências")
public class ComentarioController {

    private final ComentarioService comentarioService;

    @GetMapping
    @Operation(summary = "[PROTEGIDO] Listar comentários de um chamado")
    public List<ComentarioResponse> listar(@PathVariable Long chamadoId) {
        return comentarioService.listarPorChamado(chamadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PROTEGIDO] Adicionar novo comentário ao chamado")
    public ComentarioResponse adicionar(
            @PathVariable Long chamadoId,
            @RequestBody @Valid ComentarioRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return comentarioService.adicionar(chamadoId, request, jwt.getSubject());
    }
}
