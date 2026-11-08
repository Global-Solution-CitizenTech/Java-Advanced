package fiap.com.br.zelafix.historico;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistStatusRepository extends JpaRepository<HistStatus, Long> {
    List<HistStatus> findByChamadoIdChamadoOrderByDtMudancaDesc(Long idChamado);
}

