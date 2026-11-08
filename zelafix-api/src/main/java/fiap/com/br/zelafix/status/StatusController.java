package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.status.dto.StatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/status", "/api/status"})
@RequiredArgsConstructor
@Tag(name = "Status", description = "Estados do ciclo de vida dos chamados")
public class StatusController {

    private final StatusService statusService;

    @GetMapping
    @Operation(summary = "[PÚBLICO] Listar todos os status cadastrados")
    public List<StatusResponse> listar() {
        return statusService.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PÚBLICO] Buscar status por id")
    public StatusResponse buscarPorId(@PathVariable Long id) {
        return StatusResponse.fromEntity(statusService.buscarPorId(id));
    }
}
