package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.status.dto.StatusRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    public List<Status> findAll() {
        return statusRepository.findAll();
    }

    public Status findById(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado com id: " + id));
    }

    public Status findByNameOrCreate(String nome) {
        return statusRepository.findByNmStatusIgnoreCase(nome)
                .orElseGet(() -> statusRepository.save(new StatusRequest(nome).toEntity()));
    }

    @Transactional
    public Status create(StatusRequest request) {
        return statusRepository.save(request.toEntity());
    }

    @Transactional
    public Status update(Long id, StatusRequest request) {
        Status status = findById(id);
        status.setNmStatus(request.nome().toUpperCase());
        return statusRepository.save(status);
    }

    @Transactional
    public void delete(Long id) {
        statusRepository.delete(findById(id));
    }
}
