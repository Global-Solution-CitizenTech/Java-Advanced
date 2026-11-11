package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.status.dto.StatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    @Cacheable("status")
    public List<Status> findAll() {
        return statusRepository.findAll();
    }

    @Cacheable("status")
    public Status findById(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado com id: " + id));
    }

    @Cacheable(value = "status", key = "#nome.trim().toUpperCase()")
    public Status findByNameOrCreate(String nome) {
        return statusRepository.findByNmStatusIgnoreCase(nome)
                .orElseGet(() -> statusRepository.save(new StatusRequest(nome).toEntity()));
    }

    @Transactional
    @CacheEvict(value = "status", allEntries = true)
    public Status create(StatusRequest request) {
        String nome = request.nome().trim().toUpperCase();
        if (statusRepository.findByNmStatusIgnoreCase(nome).isPresent()) {
            throw new IllegalArgumentException("Status já cadastrado com o nome: " + nome);
        }
        return statusRepository.save(request.toEntity());
    }

    @Transactional
    @CacheEvict(value = "status", allEntries = true)
    public Status update(Long id, StatusRequest request) {
        Status status = findById(id);
        String novoNome = request.nome().trim().toUpperCase();
        statusRepository.findByNmStatusIgnoreCase(novoNome).ifPresent(existente -> {
            if (!existente.getIdStatus().equals(id)) {
                throw new IllegalArgumentException("Já existe um status cadastrado com o nome: " + novoNome);
            }
        });
        status.setNmStatus(novoNome);
        return statusRepository.save(status);
    }

    @Transactional
    @CacheEvict(value = "status", allEntries = true)
    public void delete(Long id) {
        statusRepository.delete(findById(id));
    }
}
