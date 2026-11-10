package fiap.com.br.zelafix.categoria;

import fiap.com.br.zelafix.categoria.dto.CategoriaRequest;
import fiap.com.br.zelafix.exception.BusinessException;
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
        String nome = request.nome().trim();
        if (categoriaRepository.findByNmCategoriaIgnoreCase(nome).isPresent()) {
            throw new BusinessException("Categoria já cadastrada com o nome: " + nome);
        }
        return categoriaRepository.save(new Categoria(null, nome));
    }

    @Transactional
    public Categoria update(Long id, CategoriaRequest request) {
        Categoria categoria = findById(id);
        String novoNome = request.nome().trim();
        categoriaRepository.findByNmCategoriaIgnoreCase(novoNome).ifPresent(existente -> {
            if (!existente.getIdCategoria().equals(id)) {
                throw new BusinessException("Já existe uma categoria cadastrada com o nome: " + novoNome);
            }
        });
        categoria.setNmCategoria(novoNome);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void delete(Long id) {
        categoriaRepository.delete(findById(id));
    }
}
