package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.categoria.dto.CategoriaResponse;
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
@RequestMapping("/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Consulta do catálogo e gestão de categorias de zeladoria urbana")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "[PÚBLICO] Listar todas as categorias cadastradas")
    public List<CategoriaResponse> findAll() {
        return categoriaService.findAll().stream().map(CategoriaResponse::fromEntity).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "[PÚBLICO] Buscar categoria por id")
    public CategoriaResponse findById(@PathVariable Long id) {
        return CategoriaResponse.fromEntity(categoriaService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Criar nova categoria")
    public CategoriaResponse create(@Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.fromEntity(categoriaService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Atualizar categoria")
    public CategoriaResponse update(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return CategoriaResponse.fromEntity(categoriaService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "[ADMIN] Deletar categoria")
    public void delete(@PathVariable Long id) {
        categoriaService.delete(id);
    }
}
