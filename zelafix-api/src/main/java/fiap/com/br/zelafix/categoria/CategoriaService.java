package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Cacheable("categorias")
    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    @Cacheable("categorias")
    public Categoria findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
    }

    @Transactional
    @CacheEvict(value = "categorias", allEntries = true)
    public Categoria create(CategoriaRequest request) {
        String nome = request.nome().trim();
        if (categoriaRepository.findByNmCategoriaIgnoreCase(nome).isPresent()) {
            throw new IllegalArgumentException("Categoria já cadastrada com o nome: " + nome);
        }
        return categoriaRepository.save(request.toEntity());
    }

    @Transactional
    @CacheEvict(value = "categorias", allEntries = true)
    public Categoria update(Long id, CategoriaRequest request) {
        Categoria categoria = findById(id);
        String novoNome = request.nome().trim();
        categoriaRepository.findByNmCategoriaIgnoreCase(novoNome).ifPresent(existente -> {
            if (!existente.getIdCategoria().equals(id)) {
                throw new IllegalArgumentException("Já existe uma categoria cadastrada com o nome: " + novoNome);
            }
        });
        categoria.setNmCategoria(novoNome);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    @CacheEvict(value = "categorias", allEntries = true)
    public void delete(Long id) {
        categoriaRepository.delete(findById(id));
    }
}
