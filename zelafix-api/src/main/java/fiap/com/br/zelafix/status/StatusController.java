package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.status.dto.StatusRequest;
import fiap.com.br.zelafix.status.dto.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/status")
@RequiredArgsConstructor
@Tag(name = "Status", description = "Consulta do catálogo e gestão de status dos chamados")
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    @Operation(summary = "[PÚBLICO] Listar todos os status cadastrados")
    public List<StatusResponse> findAll() {
        return statusService.findAll().stream().map(StatusResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PÚBLICO] Buscar status por id")
    public StatusResponse findById(@PathVariable Long id) {
        return StatusResponse.fromEntity(statusService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Criar novo status")
    public StatusResponse create(@Valid @RequestBody StatusRequest request) {
        return StatusResponse.fromEntity(statusService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Atualizar status")
    public StatusResponse update(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return StatusResponse.fromEntity(statusService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Deletar status")
    public void delete(@PathVariable Long id) {
        statusService.delete(id);
    }
}
