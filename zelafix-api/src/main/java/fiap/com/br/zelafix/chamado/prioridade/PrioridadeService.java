package fiap.com.br.zelafix.chamado.prioridade;

import fiap.com.br.zelafix.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrioridadeService {

    private final PrioridadeRepository prioridadeRepository;

    @Cacheable(value = "prioridades", key = "#prioridade.name()")
    public Prioridade findByNome(PrioridadeEnum prioridade) {
        return prioridadeRepository.findByNmPrioridade(prioridade)
                .orElseThrow(() -> new ResourceNotFoundException("Prioridade não encontrada: " + prioridade));
    }
}
