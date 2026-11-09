package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public List<Categoria> findAll() {
        return categoriaRepository.findAll();
    }

    public Categoria findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada com id: " + id));
    }

    public Categoria findByNameOrCreate(String nome) {
        return categoriaRepository.findByNmCategoriaIgnoreCase(nome)
                .orElseGet(() -> categoriaRepository.save(new CategoriaRequest(nome).toEntity()));
    }

    @Transactional
    public Categoria create(CategoriaRequest request) {
        return categoriaRepository.save(request.toEntity());
    }

    @Transactional
    public Categoria update(Long id, CategoriaRequest request) {
        Categoria categoria = findById(id);
        categoria.setNmCategoria(request.nome());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void delete(Long id) {
        categoriaRepository.delete(findById(id));
    }
}
