package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Consulta do catálogo de categorias de zeladoria urbana")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "[PÚBLICO] Listar todas as categorias cadastradas")
    public List<CategoriaResponse> listar() {
        return categoriaService.listarTodas();
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PÚBLICO] Buscar categoria por id")
    public CategoriaResponse buscarPorId(@PathVariable Long id) {
        return CategoriaResponse.fromEntity(categoriaService.buscarPorId(id));
    }
}
