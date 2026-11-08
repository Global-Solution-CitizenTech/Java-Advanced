package fiap.com.br.zelafix.status;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import fiap.com.br.zelafix.status.dto.StatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    public List<StatusResponse> listarTodos() {
        return statusRepository.findAll().stream()
                .map(StatusResponse::fromEntity)
                .toList();
    }

    public Status buscarPorId(Long id) {
        return statusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado com id: " + id));
    }

    public Status buscarPorNomeOuCriar(String nome) {
        return statusRepository.findByNmStatusIgnoreCase(nome.trim())
                .orElseGet(() -> statusRepository.save(Status.builder()
                        .nmStatus(nome.trim())
                        .build()));
    }
}

