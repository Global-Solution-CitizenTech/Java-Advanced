package fiap.com.br.zelafix.historico;

import fiap.com.br.zelafix.historico.dto.HistoricoRequest;
import fiap.com.br.zelafix.historico.dto.HistoricoResponse;
import fiap.com.br.zelafix.historico.dto.HistoricoUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/historicos")
@RequiredArgsConstructor
@Tag(name = "Históricos", description = "CRUD de eventos de auditoria e timeline de chamados")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Listar eventos de histórico com paginação")
    public Page<HistoricoResponse> findAll(
            @PageableDefault(size = 20, sort = "dtMudanca", direction = Sort.Direction.DESC) Pageable pageable) {
        return historicoService.findAll(pageable);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar evento de histórico por ID")
    public HistoricoResponse findById(@PathVariable Long id) {
        return historicoService.findById(id);
    }

    @GetMapping("/chamado/{chamadoId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('CIDADAO', 'GESTOR', 'ADMIN')")
    @Operation(summary = "Listar histórico completo de um chamado em ordem cronológica decrescente")
    public List<HistoricoResponse> findByChamadoId(@PathVariable Long chamadoId) {
        return historicoService.findByChamadoId(chamadoId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    @Operation(summary = "[GESTOR / ADMIN] Registrar novo evento de histórico/status para um chamado")
    public HistoricoResponse create(@RequestBody @Valid HistoricoRequest request, @AuthenticationPrincipal Jwt jwt) {
        return historicoService.create(request, jwt.getSubject());
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Atualizar observação e status de um evento de histórico")
    public HistoricoResponse update(@PathVariable Long id, @RequestBody @Valid HistoricoUpdateRequest request) {
        return historicoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Excluir evento de histórico (exceto o evento inicial de criação)")
    public void delete(@PathVariable Long id) {
        historicoService.delete(id);
    }
}
