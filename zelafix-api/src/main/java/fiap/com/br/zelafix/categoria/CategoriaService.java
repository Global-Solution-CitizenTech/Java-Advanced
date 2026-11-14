package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Cacheable(value = "categorias", key = "#categoria.name()")
    public Categoria findByNome(CategoriaEnum categoria) {
        return categoriaRepository.findByNmCategoria(categoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + categoria));
    }
}
