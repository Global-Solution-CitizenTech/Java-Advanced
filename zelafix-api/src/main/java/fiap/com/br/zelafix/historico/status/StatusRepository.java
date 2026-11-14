package fiap.com.br.zelafix.historico.status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByNmStatus(StatusChamadoEnum nmStatus);
}
