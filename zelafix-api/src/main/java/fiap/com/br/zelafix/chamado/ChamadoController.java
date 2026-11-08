package fiap.com.br.zelafix.chamado;

import fiap.com.br.zelafix.chamado.dto.ChamadoCreateRequest;
import fiap.com.br.zelafix.chamado.dto.ChamadoResponse;
import fiap.com.br.zelafix.chamado.dto.ChamadoUpdateRequest;
import fiap.com.br.zelafix.chamado.dto.EstatisticasResponse;
import fiap.com.br.zelafix.chamado.dto.StatusUpdateRequest;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping({"/chamados", "/api/chamados"})
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Operações completas de CRUD e gestão de chamados de zeladoria urbana")
public class ChamadoController {

    private final ChamadoService chamadoService;

    @GetMapping
    @Operation(summary = "[PROTEGIDO] Listar chamados com paginação (Pageable) e filtros dinâmicos")
    public Page<ChamadoResponse> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Boolean apenasMeus,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20, sort = "dtAbertura", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String email = jwt != null ? jwt.getSubject() : null;
        return chamadoService.listar(status, categoria, busca, apenasMeus, email, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PROTEGIDO] Buscar chamado por ID com dados completos e histórico de auditoria")
    public ChamadoResponse buscarPorId(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt != null ? jwt.getSubject() : null;
        return chamadoService.buscarPorId(id, email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[PROTEGIDO] Abrir novo chamado de zeladoria (dispara triagem assíncrona com RabbitMQ + Spring AI)")
    public ChamadoResponse criar(
            @RequestBody @Valid ChamadoCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.criar(request, jwt.getSubject());
    }

    @PutMapping("/{id}")
    @Operation(summary = "[PROTEGIDO] Atualizar dados do chamado")
    public ChamadoResponse atualizar(
            @PathVariable Long id,
            @RequestBody @Valid ChamadoUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.atualizar(id, request, jwt.getSubject());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "[GESTOR] Atualizar status do chamado com registro de histórico e parecer de resolução")
    public ChamadoResponse atualizarStatus(
            @PathVariable Long id,
            @RequestBody @Valid StatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chamadoService.atualizarStatus(id, request, jwt.getSubject());
    }

    @PostMapping({"/{id}/apoiar", "/{id}/voto"})
    @Operation(summary = "[PROTEGIDO] Apoiar/votar em um chamado (ou desfazer apoio) via RabbitMQ assíncrono")
    public ChamadoResponse apoiar(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return chamadoService.apoiar(id, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "[GESTOR] Excluir um chamado")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        chamadoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "[PÚBLICO] Obter estatísticas consolidadas para os cards do dashboard")
    public EstatisticasResponse obterEstatisticas() {
        return chamadoService.obterEstatisticas();
    }
}
