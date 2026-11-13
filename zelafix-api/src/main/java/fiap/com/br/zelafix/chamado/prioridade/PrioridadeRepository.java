package fiap.com.br.zelafix.chamado.prioridade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrioridadeRepository extends JpaRepository<Prioridade, Long> {
    Optional<Prioridade> findByNmPrioridadeIgnoreCase(String nmPrioridade);
}
