package fiap.com.br.zelafix.chamado.prioridade;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrioridadeRepository extends JpaRepository<Prioridade, Long> {
    Optional<Prioridade> findByNmPrioridade(PrioridadeEnum nmPrioridade);
}
