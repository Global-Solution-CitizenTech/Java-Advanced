package fiap.com.br.zelafix.endereco;

import fiap.com.br.zelafix.endereco.dto.EnderecoRequest;
import fiap.com.br.zelafix.endereco.dto.EnderecoResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enderecos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('GESTOR')")
@Tag(name = "Endereco", description = "Gerenciamento de endereços municipais")
public class EnderecoController {

    private final EnderecoService enderecoService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[GESTOR] Listar todos os endereços com paginação e ordenação")
    public Page<EnderecoResponse> findAll(@PageableDefault(size = 10, page = 0, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return enderecoService.findAll(pageable)
                .map(EnderecoResponse::fromEntity);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[GESTOR] Buscar endereço por ID")
    public EnderecoResponse findById(@PathVariable Long id) {
        return EnderecoResponse.fromEntity(enderecoService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "[GESTOR] Criar endereço")
    public EnderecoResponse create(@Valid @RequestBody EnderecoRequest enderecoRequest) {
        return EnderecoResponse.fromEntity(enderecoService.create(enderecoRequest));
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "[GESTOR] Atualizar endereço")
    public EnderecoResponse update(@PathVariable Long id, @Valid @RequestBody EnderecoRequest enderecoRequest) {
        return EnderecoResponse.fromEntity(enderecoService.update(id, enderecoRequest));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "[GESTOR] Deletar endereço")
    public void delete(@PathVariable Long id) {
        enderecoService.delete(id);
    }
}

