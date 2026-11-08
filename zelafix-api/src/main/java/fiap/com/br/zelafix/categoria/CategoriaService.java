package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaResponse;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAll().stream()
                .map(CategoriaResponse::fromEntity)
                .toList();
    }

    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
    }

    public Categoria buscarPorNomeOuCriar(String nome) {
        return categoriaRepository.findByNmCategoriaIgnoreCase(nome.trim())
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nmCategoria(nome.trim())
                        .build()));
    }
}
