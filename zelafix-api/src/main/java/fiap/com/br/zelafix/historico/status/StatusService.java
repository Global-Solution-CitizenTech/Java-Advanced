package fiap.com.br.zelafix.historico.status;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatusService {

    private final StatusRepository statusRepository;

    @Cacheable(value = "status", key = "#status.name()")
    public Status findByNome(StatusChamadoEnum status) {
        return statusRepository.findByNmStatus(status)
                .orElseThrow(() -> new ResourceNotFoundException("Status não encontrado: " + status));
    }
}
