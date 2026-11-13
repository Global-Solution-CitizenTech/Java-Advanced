package fiap.com.br.zelafix.chamado.prioridade;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrioridadeService {

    private final PrioridadeRepository prioridadeRepository;

    @Cacheable(value = "prioridades", key = "#nome.trim().toUpperCase()")
    public Prioridade findByNameOrCreate(String nome) {
        return prioridadeRepository.findByNmPrioridadeIgnoreCase(nome.trim())
                .orElseGet(() -> prioridadeRepository.save(Prioridade.builder().nmPrioridade(nome.trim().toUpperCase()).build()));
    }
}
